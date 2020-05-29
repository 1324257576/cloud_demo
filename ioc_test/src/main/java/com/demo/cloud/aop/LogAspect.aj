package com.demo.cloud.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author lqq
 * @date 2020/5/13
 */

@Component
@Aspect
public class LogAspect {

//    @Pointcut("execution(* com.demo.cloud.service.ShowService.show(..))")
//    public void pointcut() {
//    }

    @Pointcut("@annotation(com.demo.cloud.aop.LogAop)")
    public void around() {
    }

    @Around("around()")
    public Object before(ProceedingJoinPoint joinPoint) throws Throwable {

        System.out.println("Log before");
        Object proceed = joinPoint.proceed();
        System.out.println("Log after");
        return proceed;

    }

}
