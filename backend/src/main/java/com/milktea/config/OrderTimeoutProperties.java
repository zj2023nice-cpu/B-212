package com.milktea.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "order.timeout")
public class OrderTimeoutProperties {

    private int minutes = 15;

    private int cronIntervalSeconds = 60;

    private String cancelReason = "超时未支付";
}
