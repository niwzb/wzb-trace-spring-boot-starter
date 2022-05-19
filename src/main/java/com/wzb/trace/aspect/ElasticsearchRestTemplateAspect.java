package com.wzb.trace.aspect;

import com.wzb.trace.network.TraceTools;
import com.wzb.trace.report.WzbTrace;
import com.wzb.trace.report.WzbTraceStorage;
import com.wzb.trace.utils.DateUtil;
import com.wzb.trace.utils.StringUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.elasticsearch.core.DocumentOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchOperations;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Aspect
public class ElasticsearchRestTemplateAspect implements ClassAspect, ApplicationListener<ApplicationReadyEvent> {

    private final WzbTraceStorage wzbTraceStorage;

    private final Set<String> methodSet = new HashSet<>();

    private static volatile boolean INITIALIZE_ON_READY = false;

    public ElasticsearchRestTemplateAspect(WzbTraceStorage wzbTraceStorage) {
        this.wzbTraceStorage = wzbTraceStorage;
        initialize();
    }

    @Pointcut("execution(* org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate.*(..))")
    public void doSwitch() {
    }

    @Around("doSwitch()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object[] args = point.getArgs();
        Object result = null;
        MethodSignature signature = (MethodSignature) point.getSignature();
        long start = 0;
        int status = 200;
        if (INITIALIZE_ON_READY && methodSet.contains(signature.getMethod().getName()) && StringUtil.isBlank(TraceTools.ES_FLAG)) {
            TraceTools.put(TraceTools.ES_FLAG, TraceTools.get(TraceTools.X_TRACE_ID));
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
                        .project(TraceTools.ES_FLAG)
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
        Stream.of(SearchOperations.class.getMethods()).map(Method::getName).forEach(methodSet::add);
        Stream.of(DocumentOperations.class.getMethods()).map(Method::getName).forEach(methodSet::add);
        Stream.of(ElasticsearchOperations.class.getMethods()).map(Method::getName).forEach(methodSet::add);

        Set<String> filterMethodSet = new HashSet<String>(){{
            add("getElasticsearchConverter");
            add("getIndexCoordinatesFor");
            add("refresh");
            add("getDocument");
            add("stringIdRepresentation");
        }};
        methodSet.removeIf(filterMethodSet::contains);
    }

    @Override
    public WzbTraceStorage getWzbTraceStorage() {
        return this.wzbTraceStorage;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        INITIALIZE_ON_READY = true;
    }
}
