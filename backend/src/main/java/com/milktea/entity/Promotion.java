package com.milktea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("promotions")
public class Promotion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer ruleType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountValue;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer scopeType;
    private String scopeIds;
    private Integer status;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static final int RULE_TYPE_FULL_REDUCTION = 1;
    public static final int RULE_TYPE_FULL_DISCOUNT = 2;

    public static final int SCOPE_TYPE_ALL = 0;
    public static final int SCOPE_TYPE_SPECIFIC_CATEGORY = 1;
    public static final int SCOPE_TYPE_SPECIFIC_PRODUCT = 2;

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;
}
