package com.milktea.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.milktea.entity.Promotion;
import com.milktea.mapper.PromotionMapper;
import com.milktea.service.PromotionService;
import com.milktea.util.PromotionCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromotionServiceImpl extends ServiceImpl<PromotionMapper, Promotion> implements PromotionService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionServiceImpl.class);

    private final PromotionCalculator promotionCalculator = new PromotionCalculator();

    @Override
    public Promotion createPromotion(Promotion promotion) {
        if (promotion.getStatus() == null) {
            promotion.setStatus(Promotion.STATUS_ENABLED);
        }
        if (promotion.getSort() == null) {
            promotion.setSort(0);
        }
        save(promotion);
        logger.info("促销活动创建成功: id={}, name={}", promotion.getId(), promotion.getName());
        return promotion;
    }

    @Override
    public Page<Promotion> listPromotions(Integer page, Integer pageSize, Integer status) {
        Page<Promotion> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Promotion> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Promotion::getStatus, status);
        }
        wrapper.orderByAsc(Promotion::getSort).orderByDesc(Promotion::getCreateTime);
        return page(pageParam, wrapper);
    }

    @Override
    public Promotion updatePromotionStatus(Long id, Integer status) {
        Promotion promotion = getById(id);
        if (promotion == null) {
            throw new IllegalArgumentException("促销活动不存在");
        }
        promotion.setStatus(status);
        updateById(promotion);
        logger.info("促销活动状态更新: id={}, status={}", id, status);
        return promotion;
    }

    @Override
    public List<Promotion> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        return list(new LambdaQueryWrapper<Promotion>()
                .eq(Promotion::getStatus, Promotion.STATUS_ENABLED)
                .le(Promotion::getStartTime, now)
                .ge(Promotion::getEndTime, now)
                .orderByAsc(Promotion::getSort));
    }

    @Override
    public PromotionCalculator.PromotionResult calculateBestPromotion(List<PromotionCalculator.CartItemInfo> cartItems) {
        List<Promotion> activePromotions = getActivePromotions();
        return promotionCalculator.calculateBestPromotion(activePromotions, cartItems, LocalDateTime.now());
    }

    @Override
    public PromotionCalculator.PromotionHint calculateHint(List<PromotionCalculator.CartItemInfo> cartItems) {
        List<Promotion> activePromotions = getActivePromotions();
        return promotionCalculator.calculateHint(activePromotions, cartItems, LocalDateTime.now());
    }
}
