package com.demo.cloud.controller;

import com.demo.cloud.common.Result;
import com.demo.cloud.model.Payment;
import com.demo.cloud.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author lqq
 * @date 2020/4/3
 */
@RestController
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/save")
    public Result<Integer> save(@RequestBody Payment payment) {
        int result = paymentService.save(payment);

        return new Result<>(1, payment.toString(), result);
    }


    @PutMapping("/update")
    public Result<Integer> update(@RequestBody Payment payment) {
        int result = paymentService.update(payment);
        return new Result<>(1, payment.toString(), result);
    }


    @GetMapping("/find")
    public Result<Payment> find(@RequestParam("id") Long id) {
        Payment result = paymentService.findById(id);
        return new Result<>(1, "ok", result);
    }
}
