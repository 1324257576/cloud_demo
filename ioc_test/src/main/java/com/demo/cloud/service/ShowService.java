package com.demo.cloud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lqq
 * @date 2020/5/13
 */
@Service
public class ShowService {

    @Autowired
    UserService userService;

    public void show() {
        System.out.println(userService.userName());
    }
}
