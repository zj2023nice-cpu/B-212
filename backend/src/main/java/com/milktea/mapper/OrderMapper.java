package com.milktea.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.milktea.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Update("UPDATE orders SET status = 3, cancel_reason = #{cancelReason}, update_time = NOW() WHERE id = #{id} AND status = 0")
    int cancelExpiredOrder(@Param("id") Long id, @Param("cancelReason") String cancelReason);

    @Select("SELECT * FROM orders WHERE status = 0 AND create_time < #{deadline}")
    List<Order> findExpiredUnpaidOrders(@Param("deadline") LocalDateTime deadline);
}
