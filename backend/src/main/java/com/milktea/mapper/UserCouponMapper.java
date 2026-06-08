package com.milktea.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.milktea.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    @Update("UPDATE user_coupons SET status = 1, order_id = #{orderId}, use_time = NOW() WHERE id = #{userCouponId} AND user_id = #{userId} AND status = 0")
    int useCoupon(@Param("userCouponId") Long userCouponId, @Param("userId") Long userId, @Param("orderId") Long orderId);

    @Update("UPDATE user_coupons SET status = 0, order_id = NULL, use_time = NULL WHERE id = #{userCouponId} AND user_id = #{userId} AND status = 1")
    int releaseCoupon(@Param("userCouponId") Long userCouponId, @Param("userId") Long userId);
}
