package com.milktea.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.milktea.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    @Update("UPDATE coupons SET used_count = used_count + 1 WHERE id = #{couponId} AND used_count < total_count AND status = 1")
    int incrementUsedCount(@Param("couponId") Long couponId);

    @Update("UPDATE coupons SET used_count = used_count - 1 WHERE id = #{couponId} AND used_count > 0")
    int decrementUsedCount(@Param("couponId") Long couponId);
}
