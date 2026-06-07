package com.milktea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.milktea.common.Result;
import com.milktea.entity.CartItem;
import com.milktea.entity.Product;
import com.milktea.exception.InsufficientStockException;
import com.milktea.mapper.CartItemMapper;
import com.milktea.mapper.ProductMapper;
import com.milktea.service.ProductService;
import com.milktea.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

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

    @GetMapping
    public Result<List<CartItem>> getCart() {
        return Result.success(cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, getCurrentUserId())));
    }

    @PostMapping
    public Result<String> addToCart(@RequestBody CartItem cartItem) {
        cartItem.setUserId(getCurrentUserId());
        
        Product product = productMapper.selectById(cartItem.getProductId());
        if (product == null) {
            return Result.error("Product not found");
        }

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
