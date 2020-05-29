package com.demo.cloud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lqq
 * @date 2020/4/3
 */
@RestController
public class IndexController {

    @Value("${server.port}")
    private int port;


    @GetMapping("/hi")
    public String hi(@RequestParam("id") int id) {
        return String.format("port=%d,id=%d", port, id);
    }
}
