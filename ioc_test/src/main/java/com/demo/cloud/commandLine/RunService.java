package com.demo.cloud.commandLine;

import com.demo.cloud.service.ShowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

/**
 * @author lqq
 * @date 2020/5/13
 */
//@Service
public class RunService implements CommandLineRunner {
    @Autowired
    ShowService showService;

    @Override
    public void run(String... args) throws Exception {
        showService.show();
    }
}
