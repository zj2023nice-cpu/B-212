package com.milktea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milktea.common.Result;
import com.milktea.dto.CartGroupVO;
import com.milktea.entity.CartItem;
import com.milktea.entity.Product;
import com.milktea.exception.InsufficientStockException;
import com.milktea.mapper.CartItemMapper;
import com.milktea.mapper.ProductMapper;
import com.milktea.service.ProductService;
import com.milktea.service.UserService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductMapper productMapper;

    private Long getCurrentUserId() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByUsername(username).getId();
    }

    private BigDecimal calculateUnitPrice(Product product, String specsJson) {
        BigDecimal basePrice = product.getPrice();
        if (product.getSpecPriceRules() == null || product.getSpecPriceRules().isBlank()) {
            return basePrice;
        }
        try {
            Map<String, Object> rules = objectMapper.readValue(product.getSpecPriceRules(),
                    new TypeReference<Map<String, Object>>() {});
            Map<String, Object> specs = objectMapper.readValue(specsJson,
                    new TypeReference<Map<String, Object>>() {});

            BigDecimal markup = BigDecimal.ZERO;

            Object sizeVal = specs.get("size");
            if (sizeVal != null && rules.containsKey("size")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sizeRules = (Map<String, Object>) rules.get("size");
                Object sizeMarkup = sizeRules.get(String.valueOf(sizeVal));
                if (sizeMarkup != null) {
                    markup = markup.add(new BigDecimal(String.valueOf(sizeMarkup)));
                }
            }

            Object toppingVal = specs.get("topping");
            if (toppingVal != null && rules.containsKey("topping")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> toppingRules = (Map<String, Object>) rules.get("topping");
                @SuppressWarnings("unchecked")
                List<String> toppings = (List<String>) toppingVal;
                for (String t : toppings) {
                    Object tMarkup = toppingRules.get(t);
                    if (tMarkup != null) {
                        markup = markup.add(new BigDecimal(String.valueOf(tMarkup)));
                    }
                }
            }

            return basePrice.add(markup);
        } catch (Exception e) {
            logger.warn("解析规格加价规则失败: productId={}, rules={}, specs={}, error={}",
                    product.getId(), product.getSpecPriceRules(), specsJson, e.getMessage());
            return basePrice;
        }
    }

    @GetMapping
    public Result<List<CartGroupVO>> getCart() {
        Long userId = getCurrentUserId();
        List<CartItem> cartItems = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));

        Map<Long, CartGroupVO> groupMap = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            CartGroupVO group = groupMap.get(item.getProductId());
            if (group == null) {
                Product product = productMapper.selectById(item.getProductId());
                group = new CartGroupVO();
                group.setProductId(item.getProductId());
                group.setProductName(product != null ? product.getName() : "");
                group.setImage(product != null ? product.getImage() : "");
                group.setPrice(product != null ? product.getPrice() : java.math.BigDecimal.ZERO);
                group.setSpecs(new ArrayList<>());
                groupMap.put(item.getProductId(), group);
            }
            group.getSpecs().add(item);
        }

        return Result.success(new ArrayList<>(groupMap.values()));
    }

    @PostMapping
    public Result<String> addToCart(@RequestBody CartItem cartItem) {
        cartItem.setUserId(getCurrentUserId());
        
        Product product = productMapper.selectById(cartItem.getProductId());
        if (product == null) {
            return Result.error("Product not found");
        }

        BigDecimal calculatedPrice = calculateUnitPrice(product, cartItem.getSpecs());
        if (cartItem.getUnitPrice() != null && cartItem.getUnitPrice().compareTo(calculatedPrice) != 0) {
            logger.warn("购物车加价校验失败: 前端价格={}, 后端计算价格={}, productId={}, specs={}",
                    cartItem.getUnitPrice(), calculatedPrice, cartItem.getProductId(), cartItem.getSpecs());
            return Result.error("价格校验失败，请刷新后重试");
        }
        cartItem.setUnitPrice(calculatedPrice);

        LambdaQueryWrapper<CartItem> query = new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, cartItem.getUserId())
                .eq(CartItem::getProductId, cartItem.getProductId())
                .eq(CartItem::getSpecs, cartItem.getSpecs());
        CartItem existing = cartItemMapper.selectOne(query);
        
        int totalQuantity = cartItem.getQuantity();
        if (existing != null) {
            totalQuantity = existing.getQuantity() + cartItem.getQuantity();
        }

        try {
            productService.checkStock(cartItem.getProductId(), totalQuantity);
        } catch (InsufficientStockException e) {
            logger.warn("添加购物车时库存不足: productId={}, requested={}, available={}",
                    cartItem.getProductId(), totalQuantity, e.getAvailableStock());
            return Result.error(e.getMessage());
        }

        if (existing != null) {
            existing.setQuantity(totalQuantity);
            existing.setUnitPrice(calculatedPrice);
            cartItemMapper.updateById(existing);
        } else {
            cartItemMapper.insert(cartItem);
        }
        
        return Result.success("Added to cart");
    }

    @PutMapping("/{id}")
    public Result<String> updateQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        if (quantity <= 0) {
            return Result.error("Quantity must be greater than 0");
        }

        Long currentUserId = getCurrentUserId();
        CartItem existingCartItem = cartItemMapper.selectById(id);
        
        if (existingCartItem == null) {
            return Result.error("Cart item not found");
        }
        
        if (!existingCartItem.getUserId().equals(currentUserId)) {
            return Result.error("Not authorized to update this cart item");
        }

        try {
            productService.checkStock(existingCartItem.getProductId(), quantity);
        } catch (InsufficientStockException e) {
            logger.warn("更新购物车数量时库存不足: productId={}, requested={}, available={}",
                    existingCartItem.getProductId(), quantity, e.getAvailableStock());
            return Result.error(e.getMessage());
        }
        
        CartItem cartItem = new CartItem();
        cartItem.setId(id);
        cartItem.setQuantity(quantity);
        cartItemMapper.updateById(cartItem);
        return Result.success("Updated");
    }

    @DeleteMapping("/{id}")
    public Result<String> removeFromCart(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        CartItem existingCartItem = cartItemMapper.selectById(id);
        
        if (existingCartItem == null) {
            return Result.error("Cart item not found");
        }
        
        if (!existingCartItem.getUserId().equals(currentUserId)) {
            return Result.error("Not authorized to delete this cart item");
        }
        
        cartItemMapper.deleteById(id);
        return Result.success("Removed");
    }

    @DeleteMapping("/clear")
    public Result<String> clearCart() {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, getCurrentUserId()));
        return Result.success("Cleared");
    }
}
