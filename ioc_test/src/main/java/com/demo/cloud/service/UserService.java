package com.demo.cloud.service;

import com.demo.cloud.aop.LogAop;
import com.demo.cloud.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lqq
 * @date 2020/5/13
 */
@Service
public class UserService {

    @Autowired
    private User user;

    @LogAop
    public String userName() {
        System.out.println("user=" + user);
        return user.getName();
    }
}
