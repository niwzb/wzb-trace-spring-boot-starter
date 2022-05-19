package com.wzb.trace.network.http;

import com.wzb.trace.network.TraceTools;
import com.wzb.trace.report.WzbTrace;
import com.wzb.trace.report.WzbTraceStorage;
import com.wzb.trace.utils.DateUtil;
import com.wzb.trace.utils.IPUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

@Slf4j
public class WzbFeignTraceResponseInterceptor implements Interceptor {

    private final WzbTraceStorage wzbTraceStorage;

    public WzbFeignTraceResponseInterceptor(WzbTraceStorage wzbTraceStorage) {
        this.wzbTraceStorage = wzbTraceStorage;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response;
        ResponseBody responseBody = null;
        Headers headers = null;
        RequestBody requestBody = chain.request().body();
        int status = TraceTools.X_RESPONSE_STATUS_500;
        URL url = chain.request().url().url();
        String path = url.getPath();
        long start = System.currentTimeMillis();
        try {
            response = chain.proceed(chain.request());
            responseBody = response.body();
            headers = response.headers();
            status = response.code();
            return response;
        } catch (Throwable throwable) {
            throw throwable;
        } finally {
            String subProject = null;
            if (null != headers) {
                subProject = headers.get(TraceTools.SUB_PROJECT);
            }
            WzbTrace wzbTrace = WzbTrace.builder()
                    .clientIP(TraceTools.getLocalIp())
                    .project(null == subProject ? IPUtils.Agent.USER.getType() + "(" + url.getHost() + ")" : wzbTraceStorage.getProject())
                    .duration(System.currentTimeMillis() - start)
                    .requestSize(null == requestBody ? 0 : requestBody.contentLength())
                    .responseSize(null == responseBody ? 0 : responseBody.contentLength())
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
