package com.demo.cloud.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author lqq
 * @date 2020/4/3
 */
@Data
public class Payment implements Serializable {
    private Long id;
    private String serial;
}
