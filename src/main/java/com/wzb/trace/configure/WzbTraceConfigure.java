package com.wzb.trace.configure;

import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.wzb.trace.certificate.EscapeCertificateRestTemplate;
import com.wzb.trace.aspect.ElasticsearchRestTemplateAspect;
import com.wzb.trace.network.http.WzbFeignTraceInterceptor;
import com.wzb.trace.network.http.WzbFeignTraceResponseInterceptor;
import com.wzb.trace.network.http.WzbHttpLogInterceptor;
import com.wzb.trace.network.http.WzbHttpTraceFilter;
import com.wzb.trace.network.http.WzbHttpTraceInterceptor;
import com.wzb.trace.network.http.WzbHttpTraceResponseInterceptor;
import com.wzb.trace.report.WzbTraceStorage;
import com.wzb.trace.report.storage.WzbTraceEsStorageReport;
import com.wzb.trace.report.storage.WzbTraceRejectReport;
import feign.RequestInterceptor;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class WzbTraceConfigure {

    public WzbTraceConfigure() {

    }

    @Bean
    public FilterRegistrationBean<WzbHttpTraceFilter> wzbHttpTraceFilterRegistrationBean(WzbTraceStorage wzbTraceStorage) {

        FilterRegistrationBean<WzbHttpTraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new WzbHttpTraceFilter(wzbTraceStorage));
        registration.addUrlPatterns("*");
        registration.setName("wzbHttpTraceFilter");
        registration.setOrder(0);
        return registration;
    }

    @Bean
    public ClientHttpRequestFactory wzbClientHttpRequestFactory(ConfigurableApplicationContext ctx) {
        ConfigurableEnvironment environment = ctx.getEnvironment();
        OkHttp3ClientHttpRequestFactory requestFactory = new OkHttp3ClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Integer.parseInt(environment.getProperty("http.connect.timeout", "60000")));
        requestFactory.setReadTimeout(Integer.parseInt(environment.getProperty("http.read.timeout", "30000")));
        requestFactory.setWriteTimeout(Integer.parseInt(environment.getProperty("http.write.timeout", "20000")));
        return requestFactory;
    }

    @Bean
    public HttpMessageConverter<?> fastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        SerializerFeature[] serializerFeatures = new SerializerFeature[]{
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty,
                // SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteNullBooleanAsFalse,
                SerializerFeature.DisableCircularReferenceDetect};
        SerializeConfig serializeConfig = SerializeConfig.globalInstance;
        fastJsonConfig.setSerializeConfig(serializeConfig);
        fastJsonConfig.setSerializerFeatures(serializerFeatures);
        fastJsonConfig.setCharset(StandardCharsets.UTF_8);
        fastJsonConfig.setDateFormat("yyyy-MM-dd HH:mm:ss");
        fastConverter.setFastJsonConfig(fastJsonConfig);
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        mediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        mediaTypes.add(new MediaType("application", "*+json", StandardCharsets.UTF_8));
        fastConverter.setSupportedMediaTypes(mediaTypes);
        return fastConverter;
    }

    @Bean
    @ConfigurationProperties("wzb-trace")
    public WzbTraceStorageConfiguration wzbTraceStorageConfiguration() {
        return new WzbTraceStorageConfiguration();
    }

    @ConfigurationProperties("escape-certificate")
    @Bean
    public EscapeCertificateConfiguration escapeCertificateConfiguration() {
        return new EscapeCertificateConfiguration();
    }

    @Bean
    public WzbTraceStorage wzbTraceStorage(WzbTraceStorageConfiguration wzbTraceStorageConfiguration,
                                           ConfigurableApplicationContext ctx) throws Exception {
        WzbTraceStorage wzbTraceStorage = new WzbTraceStorage(ctx);
        wzbTraceStorage.initialize(wzbTraceStorageConfiguration);
        return wzbTraceStorage;
    }

    @Primary
    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory wzbClientHttpRequestFactory,
                                     HttpMessageConverter<?> fastJsonHttpMessageConverter,
                                     WzbTraceStorage wzbTraceStorage) {
        RestTemplate restTemplate = new RestTemplate(wzbClientHttpRequestFactory);
        restTemplate.getMessageConverters().add(0, fastJsonHttpMessageConverter);
        restTemplate.getInterceptors().add(new WzbHttpTraceInterceptor());
        restTemplate.getInterceptors().add(new WzbHttpTraceResponseInterceptor(wzbTraceStorage));
        restTemplate.getInterceptors().add(new WzbHttpLogInterceptor());
        return restTemplate;
    }

    @Primary
    @Bean
    public OkHttpClient.Builder wzbOkHttpClientResponseInterceptor(WzbTraceStorage wzbTraceStorage) {
        return new OkHttpClient().newBuilder().addInterceptor(new WzbFeignTraceResponseInterceptor(wzbTraceStorage));
    }

    @Bean
    public RequestInterceptor wzbFeignTraceInterceptor() {
        return new WzbFeignTraceInterceptor();
    }

    @Bean
    public WzbTraceEsStorageReport wzbTraceEsStorageReport(WzbTraceStorageConfiguration wzbTraceStorageConfiguration,
                                                           WzbTraceStorage wzbTraceStorage) {
        return new WzbTraceEsStorageReport(wzbTraceStorage, null == wzbTraceStorageConfiguration.getElasticsearch() ? null : wzbTraceStorageConfiguration.getElasticsearch().getNamespace());
    }

    @Bean
    public WzbTraceRejectReport wzbTraceRejectReport() {
        return new WzbTraceRejectReport();
    }

    @Bean
    public EscapeCertificateRestTemplate escapeCertificateRestTemplate(EscapeCertificateConfiguration escapeCertificateConfiguration,
                                                                       HttpMessageConverter<?> fastJsonHttpMessageConverter,
                                                                       WzbTraceStorage wzbTraceStorage)
            throws NoSuchAlgorithmException, KeyManagementException {
        return new EscapeCertificateRestTemplate(escapeCertificateConfiguration,
                Collections.singletonList(fastJsonHttpMessageConverter),
                Arrays.asList(new WzbHttpTraceInterceptor(), new WzbHttpTraceResponseInterceptor(wzbTraceStorage), new WzbHttpLogInterceptor()));
    }

    @Bean
    public ElasticsearchRestTemplateAspect elasticsearchRestTemplateAspect(WzbTraceStorage wzbTraceStorage)  {
        return new ElasticsearchRestTemplateAspect(wzbTraceStorage);
    }
}
