package com.demo.cloud.controller;

import com.demo.cloud.config.OrderInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * @author lqq
 * @date 2020/4/3
 */
@RestController
public class IndexController {


    @Autowired
    private OrderInfo orderInfo;

    @GetMapping("/orderInfo")
    public String info() {
        return "orderInfo=" + orderInfo;
    }


    public static final String PAYMENT_URL = "http://payment-service";


    @Autowired
    RestTemplate restTemplate;

    @GetMapping("/idx/{id}")
    public String index(@PathVariable("id") int id) {
        return restTemplate.getForObject(PAYMENT_URL + "/hi?id=" + id, String.class);

    }


    @Resource
    private DiscoveryClient discoveryClient;

    @GetMapping("/info")
    public Object discoveryClient() {

        return discoveryClient;
    }
}
