package com.milktea.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum DeliveryType {

    DELIVERY("DELIVERY", "外卖配送"),
    SELF_PICKUP("SELF_PICKUP", "门店自提");

    @EnumValue
    private final String code;
    private final String description;

    DeliveryType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
