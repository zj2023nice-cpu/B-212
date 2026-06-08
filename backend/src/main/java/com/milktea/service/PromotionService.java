package com.milktea.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.milktea.entity.Promotion;
import com.milktea.util.PromotionCalculator;

import java.util.List;

public interface PromotionService extends IService<Promotion> {

    Promotion createPromotion(Promotion promotion);

    Page<Promotion> listPromotions(Integer page, Integer pageSize, Integer status);

    Promotion updatePromotionStatus(Long id, Integer status);

    List<Promotion> getActivePromotions();

    PromotionCalculator.PromotionResult calculateBestPromotion(List<PromotionCalculator.CartItemInfo> cartItems);

    PromotionCalculator.PromotionHint calculateHint(List<PromotionCalculator.CartItemInfo> cartItems);
}
