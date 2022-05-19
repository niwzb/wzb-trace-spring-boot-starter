package com.wzb.trace.report;

import com.wzb.trace.configure.WzbTraceStorageConfiguration;
import com.wzb.trace.report.storage.WzbTraceEsStorageReport;
import com.wzb.trace.report.storage.WzbTraceRejectReport;
import com.wzb.trace.utils.StringUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

import java.util.ArrayList;


public class WzbTraceStorage {

    private final ConfigurableApplicationContext ctx;
    @Getter
    @Setter
    private WzbTraceStorageConfiguration configuration;

    public WzbTraceStorage(ConfigurableApplicationContext ctx) {
        this.ctx = ctx;
    }

    public WzbTraceReport getWzbTraceStorageReport() {
        assert null != configuration : "WzbTraceStorageConfiguration not initialize";
        return Boolean.TRUE.equals(configuration.getEnable()) ? ctx.getBean(Storage.match(configuration.getStorage()).getStorageClass()) : ctx.getBean(WzbTraceRejectReport.class);
    }

    public void initialize(WzbTraceStorageConfiguration configuration) throws Exception {
        this.configuration = configuration;
        if (Boolean.TRUE.equals(this.configuration.getEnable())) {
            assert StringUtil.isNotBlank(this.configuration.getStorage()) : "wzb-trace.storage not config";
            Storage.match(this.configuration.getStorage()).registerStorageReportBean(this.configuration, ctx);
        }
    }

    public String getProject() {
        return ctx.getEnvironment().getProperty("spring.application.name", "User");
    }

    public <T> T getBean(Class<? extends T> clazz) {
        return this.ctx.getBean(clazz);
    }
    
    @Getter
    enum Storage {
        ELASTICSEARCH("elasticsearch", WzbTraceEsStorageReport.class) {
            @Override
            public void registerStorageReportBean(WzbTraceStorageConfiguration configuration, ConfigurableApplicationContext ctx)
                throws Exception {
                WzbTraceEsStorageReport elasticsearch = configuration.getElasticsearch();
                assert null != elasticsearch : "WzbTraceEsStorageReport not config";
                WzbTraceEsStorageReport.Rest rest = elasticsearch.getRest();
                assert null != rest : "WzbTraceEsStorageReport don`t initialize";
                String[] hosts = rest.getUris();
                assert  null != hosts && hosts.length > 0 : "WzbTraceEsStorageReport config[uris] not found";

                ElasticsearchConverter elasticsearchConverter = ctx.getBean(ElasticsearchConverter.class);
                ArrayList<HttpHost> httpHosts = new ArrayList<>();
                for (String host : hosts) {
                    httpHosts.add(HttpHost.create(host));
                }

                RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[0]));

                if (StringUtil.isNotBlank(rest.getUsername())) {
                    Credentials credentials = new UsernamePasswordCredentials(rest.getUsername(), rest.getPassword());
                    builder.setHttpClientConfigCallback(clientConfig -> {
                        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY, credentials);
                        clientConfig.setDefaultCredentialsProvider(credentialsProvider);
                        return clientConfig;
                    });
                }

                builder.setRequestConfigCallback((requestConfig) -> {
                    if (null != rest.getConnectionTimeout()) {
                        requestConfig.setConnectTimeout((int) rest.getConnectionTimeout().toMillis());
                    }
                    if (null != rest.getReadTimeout()) {
                        requestConfig.setSocketTimeout((int) rest.getReadTimeout().toMillis());
                    }
                    return requestConfig;
                });

                RestHighLevelClient client = new RestHighLevelClient(builder);
                ctx.getBeanFactory().registerSingleton("WzbTraceElasticsearchRestTemplate", new WzbTraceEsStorageReport.WzbTraceElasticsearchRestTemplate(client, elasticsearchConverter));
            }
        }
        ;
        private final String storageName;
        private final Class<? extends WzbTraceReport> storageClass;

        Storage(String storageName, Class<? extends WzbTraceReport> storageClass) {
            this.storageName = storageName;
            this.storageClass = storageClass;
        }

        public abstract void registerStorageReportBean(WzbTraceStorageConfiguration configuration,
                                                       ConfigurableApplicationContext ctx) throws Exception;

        public static Storage match(String storageName) {
            for (Storage storage : Storage.values()) {
                if (storage.getStorageName().equals(storageName)) {
                    return storage;
                }
            }
            return ELASTICSEARCH;
        }
    }
}
