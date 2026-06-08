package com.milktea.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.milktea.entity.MemberLevel;
import com.milktea.entity.PointsRecord;
import com.milktea.mapper.MemberLevelMapper;
import com.milktea.mapper.PointsRecordMapper;
import com.milktea.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.TreeMap;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberLevelMapper, MemberLevel> implements MemberService {

    private static final Logger logger = LoggerFactory.getLogger(MemberServiceImpl.class);

    private static final int POINTS_PER_YUAN = 1;

    private static final TreeMap<Integer, Integer> LEVEL_THRESHOLDS = new TreeMap<>();
    private static final Map<Integer, BigDecimal> LEVEL_DISCOUNTS = Map.of(
            0, BigDecimal.ZERO,
            1, new BigDecimal("0.05"),
            2, new BigDecimal("0.10"),
            3, new BigDecimal("0.15")
    );

    static {
        LEVEL_THRESHOLDS.put(0, 0);
        LEVEL_THRESHOLDS.put(1, 500);
        LEVEL_THRESHOLDS.put(2, 1500);
        LEVEL_THRESHOLDS.put(3, 5000);
    }

    @Autowired
    private MemberLevelMapper memberLevelMapper;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

    @Override
    public MemberLevel getOrCreateMemberLevel(Long userId) {
        MemberLevel memberLevel = memberLevelMapper.selectOne(
                new LambdaQueryWrapper<MemberLevel>().eq(MemberLevel::getUserId, userId)
        );
        if (memberLevel == null) {
            memberLevel = new MemberLevel();
            memberLevel.setUserId(userId);
            memberLevel.setLevel(0);
            memberLevel.setTotalPoints(0);
            memberLevel.setCurrentPoints(0);
            memberLevelMapper.insert(memberLevel);
            logger.info("初始化会员等级: userId={}", userId);
        }
        return memberLevel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public PointsRecord earnPoints(Long userId, Long orderId, BigDecimal amount) {
        MemberLevel memberLevel = getOrCreateMemberLevel(userId);

        int points = amount.setScale(0, RoundingMode.DOWN).intValue() * POINTS_PER_YUAN;
        if (points <= 0) {
            return null;
        }

        memberLevel.setTotalPoints(memberLevel.getTotalPoints() + points);
        memberLevel.setCurrentPoints(memberLevel.getCurrentPoints() + points);

        int newLevel = getLevelByTotalPoints(memberLevel.getTotalPoints());
        int oldLevel = memberLevel.getLevel();
        memberLevel.setLevel(newLevel);
        memberLevelMapper.updateById(memberLevel);

        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setOrderId(orderId);
        record.setPoints(points);
        record.setType(1);
        record.setDescription("消费获得积分");
        record.setBalance(memberLevel.getCurrentPoints());
        pointsRecordMapper.insert(record);

        if (newLevel > oldLevel) {
            logger.info("会员等级升级: userId={}, oldLevel={}, newLevel={}", userId, oldLevel, newLevel);
        }

        logger.info("积分获取成功: userId={}, orderId={}, points={}, totalPoints={}, currentPoints={}",
                userId, orderId, points, memberLevel.getTotalPoints(), memberLevel.getCurrentPoints());
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void deductPoints(Long userId, Long orderId, BigDecimal amount) {
        MemberLevel memberLevel = getOrCreateMemberLevel(userId);

        int points = amount.setScale(0, RoundingMode.DOWN).intValue() * POINTS_PER_YUAN;
        if (points <= 0) {
            return;
        }

        if (memberLevel.getCurrentPoints() < points) {
            points = memberLevel.getCurrentPoints();
        }

        memberLevel.setTotalPoints(Math.max(0, memberLevel.getTotalPoints() - points));
        memberLevel.setCurrentPoints(Math.max(0, memberLevel.getCurrentPoints() - points));

        int newLevel = getLevelByTotalPoints(memberLevel.getTotalPoints());
        memberLevel.setLevel(newLevel);
        memberLevelMapper.updateById(memberLevel);

        if (points > 0) {
            PointsRecord record = new PointsRecord();
            record.setUserId(userId);
            record.setOrderId(orderId);
            record.setPoints(-points);
            record.setType(3);
            record.setDescription("订单取消扣减积分");
            record.setBalance(memberLevel.getCurrentPoints());
            pointsRecordMapper.insert(record);
        }

        logger.info("积分扣减成功: userId={}, orderId={}, points={}", userId, orderId, points);
    }

    @Override
    public Page<PointsRecord> getPointsRecords(Long userId, Integer page, Integer pageSize) {
        Page<PointsRecord> pageParam = new Page<>(page, pageSize);
        return pointsRecordMapper.selectPage(pageParam,
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getUserId, userId)
                        .orderByDesc(PointsRecord::getCreateTime));
    }

    @Override
    public BigDecimal getDiscountRate(Long userId) {
        MemberLevel memberLevel = getOrCreateMemberLevel(userId);
        return LEVEL_DISCOUNTS.getOrDefault(memberLevel.getLevel(), BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calculateDiscount(Long userId, BigDecimal amount) {
        BigDecimal rate = getDiscountRate(userId);
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        return discount;
    }

    @Override
    public Integer getLevelByTotalPoints(Integer totalPoints) {
        int level = 0;
        for (Map.Entry<Integer, Integer> entry : LEVEL_THRESHOLDS.entrySet()) {
            if (totalPoints >= entry.getValue()) {
                level = entry.getKey();
            }
        }
        return level;
    }
}
