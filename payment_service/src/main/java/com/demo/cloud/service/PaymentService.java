package com.demo.cloud.service;

import com.demo.cloud.dao.PaymentMapper;
import com.demo.cloud.model.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lqq
 * @date 2020/4/3
 */
@Service
public class PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;

    public int save(Payment payment) {
        return paymentMapper.insert(payment);
    }


    public int update(Payment payment) {
        return paymentMapper.updateById(payment);
    }

    public Payment findById(Long id) {
        return paymentMapper.selectById(id);
    }
}
