package com.wzb.trace.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Map;

@Aspect
public class JpaPropertiesAspect {

    private final Class<?> wzbJpaTraceInterceptor;

    public JpaPropertiesAspect(Class<?> wzbJpaTraceInterceptor) {
        this.wzbJpaTraceInterceptor = wzbJpaTraceInterceptor;
    }

    @Pointcut("execution(* org.springframework.boot.autoconfigure.orm.jpa.JpaProperties.setProperties(..))")
    public void setProperties() {
    }

    @Pointcut("execution(* org.springframework.boot.autoconfigure.orm.jpa.JpaProperties.getProperties(..))")
    public void getProperties() {
    }

    @Around("setProperties()")
    public Object setProperties(ProceedingJoinPoint point) throws Throwable {

        Object[] args = point.getArgs();
        Map<String, String> properties = (Map<String, String>) args[0];
        properties.put("hibernate.session_factory.statement_inspector", wzbJpaTraceInterceptor.getName());
        try {
            return point.proceed(args);
        } catch (Throwable throwable) {
            throw throwable;
        }
    }

    @Around("getProperties()")
    public Object getProperties(ProceedingJoinPoint point) throws Throwable {

        Object[] args = point.getArgs();
        try {
            Object result = point.proceed(args);
            Map<String, String> properties = (Map<String, String>) result;
            properties.put("hibernate.session_factory.statement_inspector", wzbJpaTraceInterceptor.getName());
            return result;
        } catch (Throwable throwable) {
            throw throwable;
        }
    }
}
