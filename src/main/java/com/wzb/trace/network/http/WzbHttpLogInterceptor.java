package com.wzb.trace.network.http;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

@Slf4j
public class WzbHttpLogInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String header = JSON.toJSONString(request.getHeaders());
        if (request.getHeaders().containsKey("Authorization")) {
            String tokenKey = "\"Authorization\"";
            int index = header.indexOf(tokenKey);
            int start = index + tokenKey.length() + 10;
            int end = header.indexOf("\"", start);
            String token = header.substring(start, end);
            token = token.substring(0, Math.min(5, token.length())) + "*****";
            header = header.substring(0, start) + token + header.substring(end);
        }
        if (request.getHeaders().getAccept().contains(MediaType.MULTIPART_FORM_DATA)) {
            log.info("{} {} header:{}", request.getMethodValue(), request.getURI(), header);
        } else {
            log.info("{} {} body:{} header:{}", request.getMethodValue(), request.getURI(), new String(body), header);
        }
        return execution.execute(request, body);
    }
}
