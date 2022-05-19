package com.wzb.trace.aspect;

import com.wzb.trace.network.TraceTools;
import com.wzb.trace.report.WzbTrace;
import com.wzb.trace.report.WzbTraceStorage;
import com.wzb.trace.utils.DateUtil;
import com.wzb.trace.utils.IPUtils;
import com.wzb.trace.utils.StringUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Aspect
public class RedisTemplateAspect implements ClassAspect, ApplicationListener<ApplicationReadyEvent> {

    private final WzbTraceStorage wzbTraceStorage;

    private final Set<String> methodSet = new HashSet<>();

    private static volatile boolean INITIALIZE_ON_READY = false;

    public RedisTemplateAspect(WzbTraceStorage wzbTraceStorage) {
        this.wzbTraceStorage = wzbTraceStorage;
        initialize();
    }

    @Pointcut("execution(* org.springframework.data.redis.core.*RedisTemplate.*(..))")
    public void doSwitch() {
    }

    @Around("doSwitch()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object[] args = point.getArgs();
        Object result = null;
        MethodSignature signature = (MethodSignature) point.getSignature();
        long start = 0;
        int status = 200;
        if (INITIALIZE_ON_READY && methodSet.contains(signature.getMethod().getName()) && StringUtil.isBlank(TraceTools.get(TraceTools.REDIS_FLAG))) {
            TraceTools.put(TraceTools.REDIS_FLAG, TraceTools.get(TraceTools.X_TRACE_ID));
            start = System.currentTimeMillis();
        }
        try {
            result = point.proceed(args);
            return result;
        } catch (Throwable throwable) {
            status = TraceTools.X_RESPONSE_STATUS_500;
            throw throwable;
        } finally {
            if (start > 0) {
                report(WzbTrace.builder()
                        .clientIP(TraceTools.getLocalIp())
                        .project(IPUtils.Agent.REDIS.getType())
                        .duration(System.currentTimeMillis() - start)
                        .requestSize(StringUtil.parseLength(args))
                        .responseSize(StringUtil.parseLength(result))
                        .traceId(TraceTools.get(TraceTools.X_TRACE_ID) + "|" + TraceTools.getTraceId())
                        .path(signature.getMethod().getName())
                        .status(status)
                        .timestamp(DateUtil.dateFormat(new Date(), DateUtil.UTC_DATE_FORMAT, DateUtil.TIMEZONE_UTC))
                        .build());
            }
        }
    }

    private void initialize() {
        String redisOperations = "org.springframework.data.redis.core.RedisOperations";
        try {
            Class<?> clazz = Class.forName(redisOperations);
            Stream.of(clazz.getMethods()).map(Method::getName).forEach(methodSet::add);
        } catch (ClassNotFoundException ignored) {
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        INITIALIZE_ON_READY = true;
    }

    @Override
    public WzbTraceStorage getWzbTraceStorage() {
        return this.wzbTraceStorage;
    }
}
