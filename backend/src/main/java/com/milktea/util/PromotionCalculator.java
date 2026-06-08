package com.milktea.util;

import com.milktea.entity.Promotion;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;

public class PromotionCalculator {

    public interface ScopeMatcher extends BiPredicate<Promotion, CartItemInfo> {
    }

    @Data
    public static class CartItemInfo {
        private Long productId;
        private Long categoryId;
        private BigDecimal amount;

        public CartItemInfo(Long productId, Long categoryId, BigDecimal amount) {
            this.productId = productId;
            this.categoryId = categoryId;
            this.amount = amount;
        }
    }

    @Data
    public static class PromotionResult {
        private Promotion promotion;
        private BigDecimal discountAmount;
        private boolean applied;

        public static PromotionResult empty() {
            PromotionResult r = new PromotionResult();
            r.setDiscountAmount(BigDecimal.ZERO);
            r.setApplied(false);
            return r;
        }

        public static PromotionResult of(Promotion promotion, BigDecimal discountAmount) {
            PromotionResult r = new PromotionResult();
            r.setPromotion(promotion);
            r.setDiscountAmount(discountAmount);
            r.setApplied(true);
            return r;
        }
    }

    @Data
    public static class PromotionHint {
        private Promotion currentPromotion;
        private BigDecimal currentDiscount;
        private Promotion nextTierPromotion;
        private BigDecimal amountNeededForNext;

        public static PromotionHint none() {
            return new PromotionHint();
        }

        public static PromotionHint currentOnly(Promotion promo, BigDecimal discount) {
            PromotionHint h = new PromotionHint();
            h.setCurrentPromotion(promo);
            h.setCurrentDiscount(discount);
            return h;
        }

        public static PromotionHint withNext(Promotion current, BigDecimal currentDiscount,
                                              Promotion next, BigDecimal amountNeeded) {
            PromotionHint h = new PromotionHint();
            h.setCurrentPromotion(current);
            h.setCurrentDiscount(currentDiscount);
            h.setNextTierPromotion(next);
            h.setAmountNeededForNext(amountNeeded);
            return h;
        }
    }

    private final ScopeMatcher scopeMatcher;

    public PromotionCalculator() {
        this.scopeMatcher = new DefaultScopeMatcher();
    }

    public PromotionCalculator(ScopeMatcher scopeMatcher) {
        this.scopeMatcher = scopeMatcher;
    }

    public PromotionResult calculateBestPromotion(List<Promotion> promotions,
                                                   List<CartItemInfo> cartItems,
                                                   LocalDateTime now) {
        BigDecimal totalAmount = cartItems.stream()
                .map(CartItemInfo::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return PromotionResult.empty();
        }

        List<Promotion> applicable = filterApplicable(promotions, cartItems, now);

        PromotionResult best = PromotionResult.empty();
        for (Promotion promo : applicable) {
            BigDecimal eligibleAmount = calculateEligibleAmount(promo, cartItems);
            if (eligibleAmount.compareTo(promo.getThresholdAmount()) < 0) {
                continue;
            }
            BigDecimal discount = calculateDiscount(promo, eligibleAmount);
            if (discount.compareTo(best.getDiscountAmount()) > 0) {
                best = PromotionResult.of(promo, discount);
            }
        }
        return best;
    }

    public PromotionHint calculateHint(List<Promotion> promotions,
                                        List<CartItemInfo> cartItems,
                                        LocalDateTime now) {
        BigDecimal totalAmount = cartItems.stream()
                .map(CartItemInfo::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return PromotionHint.none();
        }

        List<Promotion> applicable = filterApplicable(promotions, cartItems, now);

        List<Promotion> sortedByThreshold = new ArrayList<>(applicable);
        sortedByThreshold.sort(Comparator.comparing(Promotion::getThresholdAmount));

        Promotion currentBest = null;
        BigDecimal currentBestDiscount = BigDecimal.ZERO;
        Promotion nextTier = null;

        for (Promotion promo : sortedByThreshold) {
            BigDecimal eligibleAmount = calculateEligibleAmount(promo, cartItems);
            if (eligibleAmount.compareTo(promo.getThresholdAmount()) >= 0) {
                BigDecimal discount = calculateDiscount(promo, eligibleAmount);
                if (discount.compareTo(currentBestDiscount) > 0) {
                    currentBest = promo;
                    currentBestDiscount = discount;
                }
            } else if (nextTier == null) {
                BigDecimal discount = calculateDiscount(promo, promo.getThresholdAmount());
                if (discount.compareTo(currentBestDiscount) > 0) {
                    nextTier = promo;
                }
            }
        }

        if (currentBest != null && nextTier != null) {
            BigDecimal eligibleForNext = calculateEligibleAmount(nextTier, cartItems);
            BigDecimal needed = nextTier.getThresholdAmount().subtract(eligibleForNext);
            if (needed.compareTo(BigDecimal.ZERO) > 0) {
                return PromotionHint.withNext(currentBest, currentBestDiscount, nextTier, needed);
            }
        }

        if (currentBest != null) {
            return PromotionHint.currentOnly(currentBest, currentBestDiscount);
        }

        if (nextTier != null) {
            BigDecimal eligibleAmount = calculateEligibleAmount(nextTier, cartItems);
            BigDecimal needed = nextTier.getThresholdAmount().subtract(eligibleAmount);
            if (needed.compareTo(BigDecimal.ZERO) > 0) {
                PromotionHint hint = new PromotionHint();
                hint.setNextTierPromotion(nextTier);
                hint.setAmountNeededForNext(needed);
                return hint;
            }
        }

        return PromotionHint.none();
    }

    List<Promotion> filterApplicable(List<Promotion> promotions, List<CartItemInfo> cartItems,
                                      LocalDateTime now) {
        List<Promotion> result = new ArrayList<>();
        for (Promotion promo : promotions) {
            if (promo.getStatus() == null || promo.getStatus() != Promotion.STATUS_ENABLED) {
                continue;
            }
            if (now.isBefore(promo.getStartTime()) || now.isAfter(promo.getEndTime())) {
                continue;
            }
            boolean hasMatch = cartItems.stream()
                    .anyMatch(item -> scopeMatcher.test(promo, item));
            if (hasMatch) {
                result.add(promo);
            }
        }
        return result;
    }

    BigDecimal calculateEligibleAmount(Promotion promo, List<CartItemInfo> cartItems) {
        if (promo.getScopeType() == null || promo.getScopeType() == Promotion.SCOPE_TYPE_ALL) {
            return cartItems.stream()
                    .map(CartItemInfo::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return cartItems.stream()
                .filter(item -> scopeMatcher.test(promo, item))
                .map(CartItemInfo::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    BigDecimal calculateDiscount(Promotion promo, BigDecimal eligibleAmount) {
        if (promo.getRuleType() == Promotion.RULE_TYPE_FULL_REDUCTION) {
            BigDecimal discount = promo.getDiscountValue();
            return discount.compareTo(eligibleAmount) > 0 ? eligibleAmount : discount;
        } else if (promo.getRuleType() == Promotion.RULE_TYPE_FULL_DISCOUNT) {
            BigDecimal discount = eligibleAmount.multiply(BigDecimal.ONE.subtract(promo.getDiscountValue()));
            discount = discount.setScale(2, RoundingMode.HALF_UP);
            return discount.compareTo(eligibleAmount) > 0 ? eligibleAmount : discount;
        }
        return BigDecimal.ZERO;
    }

    public static class DefaultScopeMatcher implements ScopeMatcher {
        @Override
        public boolean test(Promotion promotion, CartItemInfo item) {
            if (promotion.getScopeType() == null || promotion.getScopeType() == Promotion.SCOPE_TYPE_ALL) {
                return true;
            }
            List<Long> ids = parseScopeIds(promotion.getScopeIds());
            if (ids.isEmpty()) {
                return promotion.getScopeType() == Promotion.SCOPE_TYPE_ALL;
            }
            if (promotion.getScopeType() == Promotion.SCOPE_TYPE_SPECIFIC_CATEGORY) {
                return item.getCategoryId() != null && ids.contains(item.getCategoryId());
            }
            if (promotion.getScopeType() == Promotion.SCOPE_TYPE_SPECIFIC_PRODUCT) {
                return item.getProductId() != null && ids.contains(item.getProductId());
            }
            return false;
        }
    }

    private static List<Long> parseScopeIds(String scopeIds) {
        if (scopeIds == null || scopeIds.isBlank()) {
            return Collections.emptyList();
        }
        try {
            String trimmed = scopeIds.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            if (trimmed.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> result = new ArrayList<>();
            for (String part : trimmed.split(",")) {
                String p = part.trim().replace("\"", "");
                if (!p.isEmpty()) {
                    result.add(Long.parseLong(p));
                }
            }
            return result;
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }
    }
}
