package com.wzb.trace.network.http;

import com.wzb.trace.network.TraceTools;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class WzbHttpTraceInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        if (!headers.containsKey(TraceTools.X_TRACE_ID)) {
            headers.set(TraceTools.X_TRACE_ID, TraceTools.get(TraceTools.X_TRACE_ID));
        }
        if (!headers.containsKey(TraceTools.PARENT_PROJECT)) {
            headers.set(TraceTools.PARENT_PROJECT, TraceTools.get(TraceTools.PROJECT));
        }
        return execution.execute(request, body);
    }
}
