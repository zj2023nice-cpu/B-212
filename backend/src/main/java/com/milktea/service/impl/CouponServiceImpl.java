package com.milktea.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.milktea.common.ResultCode;
import com.milktea.entity.Coupon;
import com.milktea.entity.UserCoupon;
import com.milktea.mapper.CouponMapper;
import com.milktea.mapper.UserCouponMapper;
import com.milktea.service.CouponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    private static final Logger logger = LoggerFactory.getLogger(CouponServiceImpl.class);
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Coupon createCoupon(Coupon coupon) {
        coupon.setCode(generateCouponCode());
        coupon.setUsedCount(0);
        if (coupon.getStatus() == null) {
            coupon.setStatus(1);
        }
        couponMapper.insert(coupon);
        logger.info("优惠券创建成功: id={}, code={}", coupon.getId(), coupon.getCode());
        return coupon;
    }

    @Override
    public Page<Coupon> listCoupons(Integer page, Integer pageSize, Integer status) {
        Page<Coupon> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        wrapper.orderByDesc(Coupon::getCreateTime);
        return couponMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Coupon updateCouponStatus(Long couponId, Integer status) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new IllegalArgumentException(ResultCode.COUPON_NOT_FOUND.getMessage());
        }
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
        logger.info("优惠券状态更新: id={}, status={}", couponId, status);
        return coupon;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon claimCoupon(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new IllegalArgumentException(ResultCode.COUPON_NOT_FOUND.getMessage());
        }

        if (coupon.getStatus() != 1) {
            throw new IllegalStateException(ResultCode.COUPON_NOT_AVAILABLE.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new IllegalStateException(ResultCode.COUPON_EXPIRED.getMessage());
        }

        if (coupon.getUsedCount() >= coupon.getTotalCount()) {
            throw new IllegalStateException(ResultCode.COUPON_STOCK_EXHAUSTED.getMessage());
        }

        Long existingCount = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId)
        );
        if (existingCount > 0) {
            throw new IllegalStateException(ResultCode.COUPON_ALREADY_CLAIMED.getMessage());
        }

        int updated = couponMapper.incrementUsedCount(couponId);
        if (updated == 0) {
            throw new IllegalStateException(ResultCode.COUPON_STOCK_EXHAUSTED.getMessage());
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);
        userCouponMapper.insert(userCoupon);

        logger.info("用户领取优惠券成功: userId={}, couponId={}", userId, couponId);
        return userCoupon;
    }

    @Override
    public Page<UserCoupon> getUserCoupons(Long userId, Integer status, Integer page, Integer pageSize) {
        Page<UserCoupon> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        if (status != null) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getCreateTime);
        return userCouponMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<UserCoupon> getAvailableCoupons(Long userId, BigDecimal orderAmount) {
        List<UserCoupon> userCoupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, 0)
        );

        LocalDateTime now = LocalDateTime.now();
        return userCoupons.stream().filter(uc -> {
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            if (coupon == null || coupon.getStatus() != 1) {
                return false;
            }
            if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
                return false;
            }
            if (orderAmount != null && coupon.getThreshold() != null && orderAmount.compareTo(coupon.getThreshold()) < 0) {
                return false;
            }
            return true;
        }).toList();
    }

    @Override
    public BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderAmount) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || userCoupon.getStatus() != 0) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null || coupon.getStatus() != 1) {
            return BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            return BigDecimal.ZERO;
        }

        if (coupon.getThreshold() != null && orderAmount.compareTo(coupon.getThreshold()) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (coupon.getType() == 1) {
            discount = coupon.getValue();
        } else if (coupon.getType() == 2) {
            discount = orderAmount.multiply(BigDecimal.ONE.subtract(coupon.getValue()));
        } else {
            discount = BigDecimal.ZERO;
        }

        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void useCoupon(Long userCouponId, Long userId, Long orderId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            throw new IllegalArgumentException(ResultCode.COUPON_NOT_FOUND.getMessage());
        }
        if (!userCoupon.getUserId().equals(userId)) {
            throw new IllegalArgumentException(ResultCode.COUPON_INVALID.getMessage());
        }
        if (userCoupon.getStatus() != 0) {
            throw new IllegalStateException(ResultCode.COUPON_ALREADY_USED.getMessage());
        }

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new IllegalArgumentException(ResultCode.COUPON_NOT_FOUND.getMessage());
        }
        if (coupon.getStatus() != 1) {
            throw new IllegalStateException(ResultCode.COUPON_NOT_AVAILABLE.getMessage());
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new IllegalStateException(ResultCode.COUPON_EXPIRED.getMessage());
        }

        int updated = userCouponMapper.useCoupon(userCouponId, userId, orderId);
        if (updated == 0) {
            throw new IllegalStateException(ResultCode.COUPON_ALREADY_USED.getMessage());
        }

        logger.info("优惠券核销成功: userCouponId={}, userId={}, orderId={}", userCouponId, userId, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseCoupon(Long userCouponId, Long userId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            return;
        }

        int updated = userCouponMapper.releaseCoupon(userCouponId, userId);
        if (updated > 0) {
            couponMapper.decrementUsedCount(userCoupon.getCouponId());
            logger.info("优惠券释放成功: userCouponId={}, userId={}", userCouponId, userId);
        }
    }

    private String generateCouponCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
