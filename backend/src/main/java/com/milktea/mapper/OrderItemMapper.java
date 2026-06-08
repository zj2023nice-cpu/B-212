package com.milktea.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.milktea.dto.CategoryPreferenceVO;
import com.milktea.dto.HotProductVO;
import com.milktea.dto.TopProductVO;
import com.milktea.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("<script>" +
            "SELECT oi.product_id AS productId, p.name AS productName, p.price AS price, " +
            "p.image AS image, p.description AS description, p.category_id AS categoryId, " +
            "c.name AS categoryName, SUM(oi.quantity) AS totalSales " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.id " +
            "JOIN products p ON oi.product_id = p.id " +
            "JOIN categories c ON p.category_id = c.id " +
            "WHERE o.status NOT IN (0, 3) " +
            "AND o.create_time &gt;= #{startDate} " +
            "AND p.status = 1 " +
            "<if test='categoryId != null'>" +
            "AND p.category_id = #{categoryId} " +
            "</if>" +
            "GROUP BY oi.product_id, p.name, p.price, p.image, p.description, p.category_id, c.name " +
            "ORDER BY totalSales DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<HotProductVO> selectHotProducts(@Param("startDate") LocalDateTime startDate,
                                         @Param("categoryId") Long categoryId,
                                         @Param("limit") int limit);

    @Select("SELECT p.category_id AS categoryId, SUM(oi.quantity) AS totalSales " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.id " +
            "JOIN products p ON oi.product_id = p.id " +
            "WHERE o.user_id = #{userId} AND o.status NOT IN (0, 3) " +
            "GROUP BY p.category_id " +
            "ORDER BY totalSales DESC")
    List<CategoryPreferenceVO> selectUserCategoryPreference(@Param("userId") Long userId);

    @Select("<script>" +
            "SELECT oi.product_id AS productId, p.name AS productName, p.price AS price, " +
            "p.image AS image, p.description AS description, p.category_id AS categoryId, " +
            "c.name AS categoryName, SUM(oi.quantity) AS totalSales " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.id " +
            "JOIN products p ON oi.product_id = p.id " +
            "JOIN categories c ON p.category_id = c.id " +
            "WHERE o.status NOT IN (0, 3) " +
            "AND o.create_time &gt;= #{startDate} " +
            "AND p.status = 1 " +
            "AND p.category_id IN " +
            "<foreach item='id' collection='categoryIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "GROUP BY oi.product_id, p.name, p.price, p.image, p.description, p.category_id, c.name " +
            "ORDER BY totalSales DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<HotProductVO> selectHotProductsByCategories(@Param("startDate") LocalDateTime startDate,
                                                     @Param("categoryIds") List<Long> categoryIds,
                                                     @Param("limit") int limit);

    @Select("SELECT oi.product_id AS productId, oi.product_name AS productName, p.image AS image, " +
            "SUM(oi.quantity) AS totalSales, SUM(oi.quantity * oi.product_price) AS totalRevenue " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.id " +
            "JOIN products p ON oi.product_id = p.id " +
            "WHERE o.status NOT IN (0, 3) AND o.create_time >= #{startDate} " +
            "GROUP BY oi.product_id, oi.product_name, p.image " +
            "ORDER BY totalSales DESC LIMIT #{limit}")
    List<TopProductVO> selectTopProducts(@Param("startDate") LocalDateTime startDate,
                                          @Param("limit") int limit);
}
