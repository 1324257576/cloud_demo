package com.demo.cloud.controller;

import com.demo.cloud.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lqq
 * @date 2020/5/13
 */
@RestController
public class ShowController {

    @Autowired

    UserService userService;

    @GetMapping("/")
    public String show() {
        return userService.userName();
    }
}
