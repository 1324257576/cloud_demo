package com.demo.cloud.aop;

import java.lang.annotation.*;

/**
 * @author lqq
 * @date 2020/5/13
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface LogAop {
}
