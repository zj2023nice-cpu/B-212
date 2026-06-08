package com.milktea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderSn;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private Long userCouponId;
    private Integer status; // 0-待支付, 1-制作中, 2-配送中, 3-已取消, 4-已送达, 5-已评价
    private String remark;
    private String cancelReason;
    private Long addressId;
    private String addressContactName;
    private String addressPhone;
    private String addressFull;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
