package com.wzb.trace.network.http;

import com.wzb.trace.network.TraceTools;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.util.Collection;
import java.util.Map;


public class WzbFeignTraceInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        Map<String, Collection<String>> headers = requestTemplate.headers();
        if (!headers.containsKey(TraceTools.X_TRACE_ID)) {
            requestTemplate.header(TraceTools.X_TRACE_ID, TraceTools.get(TraceTools.X_TRACE_ID));
        }
        if (!headers.containsKey(TraceTools.PARENT_PROJECT)) {
            requestTemplate.header(TraceTools.PARENT_PROJECT, TraceTools.get(TraceTools.PROJECT));
        }
    }
}
