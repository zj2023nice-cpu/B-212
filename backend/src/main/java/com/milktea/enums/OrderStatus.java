package com.milktea.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum OrderStatus {

    PENDING_PAYMENT("PENDING_PAYMENT", "待支付"),
    PAID("PAID", "已支付"),
    PREPARING("PREPARING", "制作中"),
    DELIVERING("DELIVERING", "配送中"),
    COMPLETED("COMPLETED", "已送达"),
    CANCELLED("CANCELLED", "已取消"),
    REVIEWED("REVIEWED", "已评价");

    @EnumValue
    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public boolean isTerminal() {
        return this == CANCELLED || this == REVIEWED;
    }
}
