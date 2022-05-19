package com.wzb.trace.aspect;

import com.wzb.trace.network.TraceTools;
import com.wzb.trace.report.WzbTrace;
import com.wzb.trace.report.WzbTraceStorage;
import com.wzb.trace.utils.DateUtil;
import com.wzb.trace.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.DatabaseMetaData;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Aspect
public class JdbcTemplateAspect implements ClassAspect, ApplicationListener<ApplicationReadyEvent> {

    private final WzbTraceStorage wzbTraceStorage;

    private final Set<String> methodSet = new HashSet<>();

    private final Map<DataSource, String> dbTypeMap = new HashMap<>();

    private final String getDataSourceMethod = "getDataSource";

    private static volatile boolean INITIALIZE_ON_READY = false;

    private Class<?> jdbcOperationsClass;


    public JdbcTemplateAspect(WzbTraceStorage wzbTraceStorage) {
        this.wzbTraceStorage = wzbTraceStorage;
        initialize();
    }

    @Pointcut("execution(* org.springframework.jdbc.core.JdbcTemplate.*(..))")
    public void doSwitch() {
    }

    @Around("doSwitch()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object target = point.getTarget();
        Class<?> clazz = target.getClass();
        Method dataSourceMethod = clazz.getMethod(getDataSourceMethod);
        DataSource dataSource = (DataSource) dataSourceMethod.invoke(target);
        Object[] args = point.getArgs();
        Object result = null;
        MethodSignature signature = (MethodSignature) point.getSignature();
        long start = 0;
        int status = 200;
        if (INITIALIZE_ON_READY && methodSet.contains(signature.getMethod().getName()) && StringUtil.isBlank(TraceTools.get(dbTypeMap.get(dataSource)))) {
            TraceTools.put(dbTypeMap.get(dataSource), TraceTools.get(TraceTools.X_TRACE_ID));
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
                        .project(dbTypeMap.get(dataSource))
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
        String jdbcOperations = "org.springframework.jdbc.core.JdbcOperations";
        try {
            this.jdbcOperationsClass = Class.forName(jdbcOperations);
            Stream.of(this.jdbcOperationsClass.getMethods()).map(Method::getName).forEach(methodSet::add);
        } catch (ClassNotFoundException ignored) {
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!INITIALIZE_ON_READY && null != this.jdbcOperationsClass) {
            INITIALIZE_ON_READY = true;
            try {
                String[] jdbcBeanNames = event.getApplicationContext().getBeanNamesForType(this.jdbcOperationsClass);
                Stream.of(jdbcBeanNames).map(event.getApplicationContext()::getBean).forEach(bean -> {
                    try {
                        Class<?> clazz = bean.getClass();
                        Method dataSourceMethod = clazz.getMethod(getDataSourceMethod);
                        DataSource dataSource = (DataSource) dataSourceMethod.invoke(bean);
                        DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
                        dbTypeMap.put(dataSource, metaData.getDatabaseProductName());
                    } catch (Throwable throwable) {
                        log.warn("Don`t get datasource type", throwable);
                    }
                });
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public WzbTraceStorage getWzbTraceStorage() {
        return this.wzbTraceStorage;
    }
}
