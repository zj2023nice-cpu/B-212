package com.milktea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.common.Result;
import com.milktea.dto.PromotionCalculateRequest;
import com.milktea.dto.PromotionCalculateResponse;
import com.milktea.entity.CartItem;
import com.milktea.entity.Product;
import com.milktea.entity.Promotion;
import com.milktea.mapper.CartItemMapper;
import com.milktea.mapper.ProductMapper;
import com.milktea.service.PromotionService;
import com.milktea.service.UserService;
import com.milktea.util.PromotionCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private static final Logger logger = LoggerFactory.getLogger(PromotionController.class);

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByUsername(username).getId();
    }

    private boolean isCurrentUserAdmin() {
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    @PostMapping
    public Result<Promotion> createPromotion(@RequestBody Promotion promotion) {
        if (!isCurrentUserAdmin()) {
            return Result.forbidden("仅管理员可创建促销活动");
        }
        Promotion created = promotionService.createPromotion(promotion);
        return Result.success(created);
    }

    @GetMapping
    public Result<Page<Promotion>> listPromotions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(promotionService.listPromotions(page, pageSize, status));
    }

    @GetMapping("/active")
    public Result<List<Promotion>> getActivePromotions() {
        return Result.success(promotionService.getActivePromotions());
    }

    @PutMapping("/{id}/status")
    public Result<Promotion> updatePromotionStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (!isCurrentUserAdmin()) {
            return Result.forbidden("仅管理员可修改促销活动状态");
        }
        Promotion updated = promotionService.updatePromotionStatus(id, status);
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result<String> deletePromotion(@PathVariable Long id) {
        if (!isCurrentUserAdmin()) {
            return Result.forbidden("仅管理员可删除促销活动");
        }
        promotionService.removeById(id);
        return Result.success("已删除");
    }

    @GetMapping("/cart-hint")
    public Result<Map<String, Object>> getCartPromotionHint() {
        Long userId = getCurrentUserId();
        List<PromotionCalculator.CartItemInfo> cartItems = buildCartItems(userId);
        if (cartItems.isEmpty()) {
            return Result.success(Map.of("hint", "", "hasPromotion", false));
        }

        PromotionCalculator.PromotionHint hint = promotionService.calculateHint(cartItems);

        Map<String, Object> result = new HashMap<>();
        result.put("hasPromotion", hint.getCurrentPromotion() != null || hint.getNextTierPromotion() != null);

        StringBuilder hintBuilder = new StringBuilder();
        if (hint.getCurrentPromotion() != null) {
            Promotion current = hint.getCurrentPromotion();
            String ruleDesc = describeRule(current);
            hintBuilder.append("已享").append(ruleDesc);
            if (hint.getCurrentDiscount() != null) {
                hintBuilder.append("，优惠¥").append(hint.getCurrentDiscount());
            }
        }
        if (hint.getNextTierPromotion() != null) {
            Promotion next = hint.getNextTierPromotion();
            String nextDesc = describeRule(next);
            if (hintBuilder.length() > 0) {
                hintBuilder.append("；");
            }
            hintBuilder.append("再购¥").append(hint.getAmountNeededForNext()).append("可享").append(nextDesc);
        }

        result.put("hint", hintBuilder.toString());
        result.put("currentPromotion", hint.getCurrentPromotion() != null ? toMap(hint.getCurrentPromotion()) : null);
        result.put("currentDiscount", hint.getCurrentDiscount());
        result.put("nextTierPromotion", hint.getNextTierPromotion() != null ? toMap(hint.getNextTierPromotion()) : null);
        result.put("amountNeededForNext", hint.getAmountNeededForNext());

        return Result.success(result);
    }

    @PostMapping("/calculate")
    public Result<PromotionCalculateResponse> calculatePromotion(@RequestBody(required = false) PromotionCalculateRequest request) {
        Long userId = getCurrentUserId();
        List<PromotionCalculator.CartItemInfo> cartItems = buildCartItems(userId);

        BigDecimal orderAmount = cartItems.stream()
                .map(PromotionCalculator.CartItemInfo::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PromotionCalculateResponse response = new PromotionCalculateResponse();
        response.setOrderAmount(orderAmount);
        response.setDiscountAmount(BigDecimal.ZERO);
        response.setFinalAmount(orderAmount);
        response.setApplied(false);

        if (cartItems.isEmpty()) {
            return Result.success(response);
        }

        PromotionCalculator.PromotionResult promoResult = promotionService.calculateBestPromotion(cartItems);
        response.setDiscountAmount(promoResult.getDiscountAmount());
        response.setApplied(promoResult.isApplied());
        response.setFinalAmount(orderAmount.subtract(promoResult.getDiscountAmount()));
        if (promoResult.getPromotion() != null) {
            response.setPromotionId(promoResult.getPromotion().getId());
            response.setPromotionName(promoResult.getPromotion().getName());
        }
        return Result.success(response);
    }

    private List<PromotionCalculator.CartItemInfo> buildCartItems(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> productIds = items.stream().map(CartItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = new HashMap<>();
        for (Long pid : productIds) {
            Product p = productMapper.selectById(pid);
            if (p != null) {
                productMap.put(pid, p);
            }
        }

        Map<Long, List<CartItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(CartItem::getProductId));

        List<PromotionCalculator.CartItemInfo> result = new ArrayList<>();
        for (Map.Entry<Long, List<CartItem>> entry : grouped.entrySet()) {
            Product product = productMap.get(entry.getKey());
            if (product == null) continue;
            BigDecimal totalAmount = entry.getValue().stream()
                    .map(item -> item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(new PromotionCalculator.CartItemInfo(product.getId(), product.getCategoryId(), totalAmount));
        }
        return result;
    }

    private String describeRule(Promotion promo) {
        if (promo.getRuleType() == Promotion.RULE_TYPE_FULL_REDUCTION) {
            return "满" + promo.getThresholdAmount().intValue() + "减" + promo.getDiscountValue().intValue();
        } else {
            return "满" + promo.getThresholdAmount().intValue() + "享" + (promo.getDiscountValue().doubleValue() * 10) + "折";
        }
    }

    private Map<String, Object> toMap(Promotion promo) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", promo.getId());
        m.put("name", promo.getName());
        m.put("ruleType", promo.getRuleType());
        m.put("thresholdAmount", promo.getThresholdAmount());
        m.put("discountValue", promo.getDiscountValue());
        m.put("scopeType", promo.getScopeType());
        m.put("scopeIds", promo.getScopeIds());
        return m;
    }
}
