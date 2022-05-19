package com.wzb.trace.certificate;

import com.wzb.trace.configure.EscapeCertificateConfiguration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

public class EscapeCertificateRestTemplate {

    private final RestTemplate restTemplate;

    public EscapeCertificateRestTemplate(EscapeCertificateConfiguration configuration,
                                         List<HttpMessageConverter<?>> httpMessageConverters,
                                         List<ClientHttpRequestInterceptor> interceptors) throws NoSuchAlgorithmException, KeyManagementException {
        this.restTemplate = buildRestTemplate(configuration, httpMessageConverters, interceptors);
    }

    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables) throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
    }

    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
    }

    public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType) throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType);
    }

    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
    }

    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
    }

    public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType) throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType);
    }

    public <T> ResponseEntity<T> exchange(RequestEntity<?> entity, Class<T> responseType) throws RestClientException {
        return restTemplate.exchange(entity, responseType);
    }

    public <T> ResponseEntity<T> exchange(RequestEntity<?> entity, ParameterizedTypeReference<T> responseType) throws RestClientException {
        return restTemplate.exchange(entity, responseType);
    }

    private RestTemplate buildRestTemplate(EscapeCertificateConfiguration configuration,
                                           List<HttpMessageConverter<?>> httpMessageConverters,
                                           List<ClientHttpRequestInterceptor> interceptors) throws NoSuchAlgorithmException, KeyManagementException {

        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        SSLSocketFactory socketFactory = context.getSocketFactory();

        SimpleClientHttpRequestFactory ssl = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection) {
                    prepareHttpsConnection((HttpsURLConnection) connection);
                }
                super.prepareConnection(connection, httpMethod);
            }

            private void prepareHttpsConnection(HttpsURLConnection connection) {
                connection.setHostnameVerifier((s, sslSession) -> true);
                try {
                    connection.setSSLSocketFactory(socketFactory);
                } catch (Exception ignored) {}
            }
        };
        ssl.setReadTimeout((int) configuration.getNoOrDefaultRest().getReadTimeout().toMillis());
        ssl.setConnectTimeout((int) configuration.getNoOrDefaultRest().getConnectTimeout().toMillis());

        RestTemplate restTemplate = new RestTemplate(ssl);
        if (null != httpMessageConverters && !httpMessageConverters.isEmpty()) {
            restTemplate.getMessageConverters().addAll(0, httpMessageConverters);
        }
        if (null != interceptors && !interceptors.isEmpty()) {
            restTemplate.getInterceptors().addAll(interceptors);
        }
        return restTemplate;
    }
}
