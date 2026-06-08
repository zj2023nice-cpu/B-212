package com.milktea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupons")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private Integer type;
    private BigDecimal threshold;
    private BigDecimal value;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalCount;
    private Integer usedCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
