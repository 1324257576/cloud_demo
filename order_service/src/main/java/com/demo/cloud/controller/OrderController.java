package com.demo.cloud.controller;

import com.demo.cloud.common.Result;
import com.demo.cloud.model.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * @author lqq
 * @date 2020/4/3
 */
@RestController
public class OrderController {


    public static final String PAYMENT_URL = "PAYMENT-SERVICE";

    @Autowired
    RestTemplate restTemplate;

    @GetMapping("/find")
    public Result<Payment> find(@RequestParam("id") Long id) {
        return restTemplate.getForObject(PAYMENT_URL + "/find?id=" + id, Result.class);
    }


    @PostMapping("/save")
    public Result<Integer> save(@RequestBody Payment payment) {
        return restTemplate.postForObject(PAYMENT_URL + "/save", payment, Result.class);
    }


    @PostMapping("/update")
    public void update(@RequestBody Payment payment) {
        restTemplate.put(PAYMENT_URL + "/update", payment);
    }

}
