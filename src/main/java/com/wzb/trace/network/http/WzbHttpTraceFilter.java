package com.wzb.trace.network.http;

import com.wzb.trace.network.ResponseWrapper;
import com.wzb.trace.network.TraceTools;
import com.wzb.trace.report.WzbTrace;
import com.wzb.trace.report.WzbTraceStorage;
import com.wzb.trace.utils.DateUtil;
import com.wzb.trace.utils.IPUtils;
import com.wzb.trace.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;


import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

@Slf4j
public class WzbHttpTraceFilter implements Filter {

    private final WzbTraceStorage wzbTraceStorage;

    public WzbHttpTraceFilter(WzbTraceStorage wzbTraceStorage) {
        this.wzbTraceStorage = wzbTraceStorage;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        TraceTools.clear();
        int status = 0;
        String clientIp = null;
        boolean isExternal = true;
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        ResponseWrapper responseWrapper = new ResponseWrapper((HttpServletResponse) servletResponse);
        String path = request.getRequestURI().replaceAll("/{2,}", "/");
        String traceId = TraceTools.getTraceId();
        String parentTraceId = request.getHeader(TraceTools.X_TRACE_ID);
        if (StringUtil.isNotBlank(parentTraceId)) {
            traceId = parentTraceId + TraceTools.TRACE_DELIMITER + traceId;
            isExternal = false;
        }
        TraceTools.put(TraceTools.X_TRACE_ID, traceId);
        TraceTools.put(TraceTools.PROJECT, wzbTraceStorage.getProject());

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, responseWrapper);
            clientIp = IPUtils.getClientIP(request);
            status = responseWrapper.getStatus();
        } catch (Throwable throwable) {
            status = TraceTools.X_RESPONSE_STATUS_500;
            throw throwable;
        } finally {
            responseWrapper.setHeader(TraceTools.SUB_PROJECT, wzbTraceStorage.getProject());

            WzbTrace wzbTrace = WzbTrace.builder()
                    .path(path)
                    .project(wzbTraceStorage.getProject())
                    .traceId(traceId)
                    .clientIP(clientIp)
                    .duration(System.currentTimeMillis() - start)
                    .status(status)
                    .requestSize(request.getContentLength())
                    .responseSize(responseWrapper.getBody().length)
                    .timestamp(DateUtil.dateFormat(new Date(), DateUtil.UTC_DATE_FORMAT, DateUtil.TIMEZONE_UTC))
                    .build();
            if (isExternal) {
                wzbTraceStorage.getWzbTraceStorageReport().report(wzbTrace, IPUtils.getClientTrace(request));
            } else {
                String parentProject = request.getHeader(TraceTools.PARENT_PROJECT);
                if (StringUtil.isBlank(parentProject)) {
                    parentProject = "User(" + clientIp + ")";
                }
                wzbTraceStorage.getWzbTraceStorageReport().report(wzbTrace, parentProject);
            }
        }
    }
}
