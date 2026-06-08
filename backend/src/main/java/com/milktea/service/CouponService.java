package com.milktea.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.milktea.entity.Coupon;
import com.milktea.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService extends IService<Coupon> {

    Coupon createCoupon(Coupon coupon);

    Page<Coupon> listCoupons(Integer page, Integer pageSize, Integer status);

    Coupon updateCouponStatus(Long couponId, Integer status);

    UserCoupon claimCoupon(Long userId, Long couponId);

    Page<UserCoupon> getUserCoupons(Long userId, Integer status, Integer page, Integer pageSize);

    List<UserCoupon> getAvailableCoupons(Long userId, BigDecimal orderAmount);

    BigDecimal calculateDiscount(Long userCouponId, BigDecimal orderAmount);

    void useCoupon(Long userCouponId, Long userId, Long orderId);

    void releaseCoupon(Long userCouponId, Long userId);
}
