package com.demo.cloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author lqq
 * @date 2020/4/10
 */
@ConfigurationProperties(prefix = "order")
@Component
@Data

public class OrderInfo {

    Integer id;
    String name;
}
