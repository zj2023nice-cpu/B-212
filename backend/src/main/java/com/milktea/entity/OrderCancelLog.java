package com.milktea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_cancel_logs")
public class OrderCancelLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String cancelReason;
    private String operator;
    private LocalDateTime createTime;
}
