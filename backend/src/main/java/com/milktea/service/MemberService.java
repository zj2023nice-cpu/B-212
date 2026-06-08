package com.milktea.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.milktea.entity.MemberLevel;
import com.milktea.entity.PointsRecord;

import java.math.BigDecimal;

public interface MemberService extends IService<MemberLevel> {

    MemberLevel getOrCreateMemberLevel(Long userId);

    PointsRecord earnPoints(Long userId, Long orderId, BigDecimal amount);

    void deductPoints(Long userId, Long orderId, BigDecimal amount);

    Page<PointsRecord> getPointsRecords(Long userId, Integer page, Integer pageSize);

    BigDecimal getDiscountRate(Long userId);

    BigDecimal calculateDiscount(Long userId, BigDecimal amount);

    Integer getLevelByTotalPoints(Integer totalPoints);
}
