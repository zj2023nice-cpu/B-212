package com.milktea.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.milktea.common.ErrorCode;
import com.milktea.common.Result;
import com.milktea.exception.BusinessException;
import com.milktea.entity.Coupon;
import com.milktea.entity.UserCoupon;
import com.milktea.mapper.CouponMapper;
import com.milktea.service.CouponService;
import com.milktea.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private static final Logger logger = LoggerFactory.getLogger(CouponController.class);

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponMapper couponMapper;

    @PostMapping
    public Result<Coupon> createCoupon(@RequestBody Coupon coupon) {
        if (!SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(ErrorCode.B0051, "仅管理员可创建优惠券");
        }
        Coupon created = couponService.createCoupon(coupon);
        return Result.success(created);
    }

    @GetMapping
    public Result<Page<Coupon>> listCoupons(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(couponService.listCoupons(page, pageSize, status));
    }

    @PutMapping("/{id}/status")
    public Result<Coupon> updateCouponStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (!SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(ErrorCode.B0052, "仅管理员可修改优惠券状态");
        }
        Coupon updated = couponService.updateCouponStatus(id, status);
        return Result.success(updated);
    }

    @PostMapping("/{id}/claim")
    public Result<UserCoupon> claimCoupon(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserCoupon userCoupon = couponService.claimCoupon(userId, id);
        return Result.success(userCoupon);
    }

    @GetMapping("/mine")
    public Result<Page<UserCoupon>> getMyCoupons(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(couponService.getUserCoupons(userId, status, page, pageSize));
    }

    @GetMapping("/available")
    public Result<List<UserCoupon>> getAvailableCoupons(
            @RequestParam(required = false) BigDecimal orderAmount) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(couponService.getAvailableCoupons(userId, orderAmount));
    }

    @PostMapping("/apply")
    public Result<Map<String, Object>> applyCoupon(@RequestBody Map<String, Object> params) {
        Long userCouponId = Long.valueOf(params.get("userCouponId").toString());
        BigDecimal orderAmount = new BigDecimal(params.get("orderAmount").toString());

        Long userId = SecurityUtils.getCurrentUserId();
        UserCoupon userCoupon = couponService.getUserCoupons(userId, null, 1, 1000)
                .getRecords().stream()
                .filter(uc -> uc.getId().equals(userCouponId))
                .findFirst()
                .orElse(null);

        if (userCoupon == null) {
            throw new BusinessException(ErrorCode.B0012, "优惠券不存在或不属于当前用户");
        }

        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new BusinessException(ErrorCode.B0019, "优惠券信息无效");
        }

        if (coupon.getThreshold() != null && orderAmount.compareTo(coupon.getThreshold()) < 0) {
            throw new BusinessException(ErrorCode.B0017, "未满足优惠券使用门槛，需满¥" + coupon.getThreshold());
        }

        BigDecimal discount = couponService.calculateDiscount(userCouponId, orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discount);

        return Result.success(Map.of(
                "discount", discount,
                "finalAmount", finalAmount,
                "couponType", coupon.getType(),
                "couponValue", coupon.getValue(),
                "threshold", coupon.getThreshold() != null ? coupon.getThreshold() : BigDecimal.ZERO
        ));
    }
}
