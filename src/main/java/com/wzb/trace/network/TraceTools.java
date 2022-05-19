package com.wzb.trace.network;

import org.slf4j.MDC;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TraceTools {

    private static final ThreadLocal<Map<String, String>> TRACE_THREAD_LOCAL = new ThreadLocal<>();

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("000");

    public static final String X_TRACE_ID = "X-Trace-Id";
    public static final String USER_AGENT = "User-Agent";
    public static final String X_TRACE_LENGTH = "X-Trace-Length";

    public static final String TRACE_DELIMITER = "|";

    public static final String X_REQUEST_PATH = "X-Request-Path";
    public static final String X_CLIENT_REAL_IP = "X-Client-IP";
    public static final String X_REQUEST_DURATION = "X_Request_Duration";
    public static final String X_REQUEST_SIZE = "X_Request_Size";
    public static final String X_RESPONSE_SIZE = "X_Response_Size";
    public static final String X_RESPONSE_STATUS = "X_Response_Status";
    public static final String TIME_STAMP = "@timestamp";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String HTTP_CLIENT_IP = "HTTP_CLIENT_IP";
    public static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
    public static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
    public static final String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";
    public static final String X_REAL_IP = "X-Real-IP";
    public static final String PROJECT = "project";
    public static final String PARENT_PROJECT = "parent-project";
    public static final String SUB_PROJECT = "sub-project";
    public static final String TRACE_SAME_USE = "same-use";
    public static final String TRACE_TYPE = "type";

    public static final String REDIS_FLAG = "redis";
    public static final String DB_FLAG = "db";
    public static final String ES_FLAG = "ElasticSearch";

    public static final int X_RESPONSE_STATUS_500 = 500;

    private volatile static String SERVER_ID;

    private volatile static String LOCAL_IP;

    public static String getTraceId() {
        initServerId();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return SERVER_ID + traceId.substring(SERVER_ID.length());
    }

    public static void put(String key, String value) {
        Map<String, String> traceContext = getDeferenceTraceContext();
        Optional.ofNullable(traceContext).ifPresent(tc -> tc.put(key, value));
    }

    public static String get(String key) {
        Map<String, String> traceContext = TRACE_THREAD_LOCAL.get();
        return null == traceContext ? null : traceContext.get(key);
    }

    public static String getLocalIp() {
        return LOCAL_IP;
    }

    public static void clear() {
        MDC.clear();
        TRACE_THREAD_LOCAL.remove();
    }

    public static Map<String, String> getCopyOfTraceContextMap() {
        Map<String, String> traceContext = TRACE_THREAD_LOCAL.get();
        return null == traceContext ? null : new HashMap<>(traceContext);
    }

    public static void setTraceContextMap(Map<String, String> traceContextMap) {
        Optional.ofNullable(traceContextMap).ifPresent(getDeferenceTraceContext()::putAll);
    }

    private static Map<String, String> getDeferenceTraceContext() {
        Map<String, String> traceContext = TRACE_THREAD_LOCAL.get();
        if (null == traceContext) {
            synchronized (X_TRACE_ID) {
                if (null == TRACE_THREAD_LOCAL.get()) {
                    traceContext = Collections.synchronizedMap(new HashMap<>());
                    TRACE_THREAD_LOCAL.set(traceContext);
                }
            }
        }
        return traceContext;
    }

    private static void initServerId() {
        if (SERVER_ID == null) {
            synchronized (X_TRACE_ID) {
                if (SERVER_ID == null) {
                    String unique;
                    try {
                        LOCAL_IP = InetAddress.getLocalHost().getHostAddress();
                        String[] netRange = LOCAL_IP.split("\\.");
                        unique = Stream.of(netRange).map(BigDecimal::new).map(DECIMAL_FORMAT::format).collect(Collectors.joining());
                    } catch (UnknownHostException ignored) {
                        unique = UUID.randomUUID().toString();
                    }
                    SERVER_ID = unique;
                }
            }
        }
    }
}
