package com.wzb.trace.network.http;

import com.wzb.trace.network.TraceTools;
import com.wzb.trace.report.WzbTrace;
import com.wzb.trace.report.WzbTraceStorage;
import com.wzb.trace.utils.DateUtil;
import com.wzb.trace.utils.IPUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;

public class WzbHttpTraceResponseInterceptor implements ClientHttpRequestInterceptor {

    private final WzbTraceStorage wzbTraceStorage;

    public WzbHttpTraceResponseInterceptor(WzbTraceStorage wzbTraceStorage) {
        this.wzbTraceStorage = wzbTraceStorage;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        int status = TraceTools.X_RESPONSE_STATUS_500;
        int responseSize = 0;
        HttpHeaders headers = null;
        URI uri = request.getURI();
        String path = uri.getPath();
        long start = System.currentTimeMillis();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            status = response.getStatusCode().value();
            headers = response.getHeaders();
            responseSize = (int) headers.getContentLength();
            return response;
        } catch (Exception e) {
            throw e;
        } finally {
            String subProject = null;
            if (null != headers) {
                List<String> projects = headers.get(TraceTools.SUB_PROJECT);
                subProject = null != projects && projects.size() > 0 ? projects.get(0) : null;
            }
            WzbTrace wzbTrace = WzbTrace.builder()
                    .clientIP(TraceTools.getLocalIp())
                    .project(null == subProject ? IPUtils.Agent.USER.getType() + "(" + uri.getHost() + ")" : wzbTraceStorage.getProject())
                    .duration(System.currentTimeMillis() - start)
                    .requestSize(body.length)
                    .responseSize(responseSize)
                    .traceId(null == subProject ? TraceTools.get(TraceTools.X_TRACE_ID) + "|" + TraceTools.getTraceId() : TraceTools.get(TraceTools.X_TRACE_ID))
                    .path(path)
                    .status(status)
                    .timestamp(DateUtil.dateFormat(new Date(), DateUtil.UTC_DATE_FORMAT, DateUtil.TIMEZONE_UTC))
                    .sameUse(null != subProject)
                    .build();
            wzbTraceStorage.getWzbTraceStorageReport().report(wzbTrace, null == subProject ? wzbTraceStorage.getProject() : null);
        }
    }
}
