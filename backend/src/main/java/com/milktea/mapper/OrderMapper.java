package com.milktea.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.milktea.dto.DailySalesVO;
import com.milktea.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Update("UPDATE orders SET status = 3, cancel_reason = #{cancelReason}, update_time = NOW() WHERE id = #{id} AND status = 0")
    int cancelExpiredOrder(@Param("id") Long id, @Param("cancelReason") String cancelReason);

    @Select("SELECT * FROM orders WHERE status = 0 AND create_time < #{deadline}")
    List<Order> findExpiredUnpaidOrders(@Param("deadline") LocalDateTime deadline);

    @Select("SELECT COUNT(*) FROM orders WHERE status NOT IN (0, 3) AND create_time >= #{startTime} AND create_time < #{endTime}")
    Long countTodayOrders(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM orders WHERE status NOT IN (0, 3) AND create_time >= #{startTime} AND create_time < #{endTime}")
    BigDecimal sumTodaySales(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM orders WHERE status IN (1, 2)")
    Long countPendingOrders();

    @Select("SELECT DATE_FORMAT(create_time, '%m-%d') AS date, COUNT(*) AS orderCount, COALESCE(SUM(pay_amount), 0) AS salesAmount " +
            "FROM orders WHERE status NOT IN (0, 3) AND create_time >= #{startDate} " +
            "GROUP BY DATE(create_time) ORDER BY DATE(create_time)")
    List<DailySalesVO> selectDailySalesTrend(@Param("startDate") LocalDateTime startDate);
}
