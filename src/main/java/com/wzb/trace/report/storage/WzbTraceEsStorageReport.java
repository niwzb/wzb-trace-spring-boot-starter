package com.wzb.trace.report.storage;

import com.wzb.trace.network.TraceTools;
import com.wzb.trace.report.WzbTrace;
import com.wzb.trace.report.WzbTraceStorage;
import com.wzb.trace.report.WzbTraceReport;
import com.wzb.trace.report.es.ThresholdConfigDoc;
import com.wzb.trace.report.es.TraceTransformDoc;
import com.wzb.trace.utils.IPUtils;
import com.wzb.trace.utils.StringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class WzbTraceEsStorageReport implements WzbTraceReport {

    private final WzbTraceStorage wzbTraceStorage;

    private final static String NAMESPACE_DEFAULT = "wzb-trace";

    private final static String COLLECT_INDEX = "-collect";

    private final static String TRANSFORM_INDEX = "-transform";

    private final static String THRESHOLD_CONFIG = "-threshold-config";

    private final static DateFormat PARTITION_FORMAT = new SimpleDateFormat("-yyyy.MM.dd");

    private volatile static String NAMESPACE;

    @Getter
    @Setter
    private String namespace;

    @Getter
    @Setter
    private Rest rest;

    public WzbTraceEsStorageReport(WzbTraceStorage wzbTraceStorage, String namespace) {
        this.wzbTraceStorage = wzbTraceStorage;
        this.namespace = namespace;
    }

    @Override
    public Runnable reportRunnable(WzbTrace wzbTrace, String parentProject) {
        return new ReportTraceRunnable(initialize(), namespace, wzbTrace, parentProject);
    }

    @Override
    public Runnable reportRunnable(WzbTrace wzbTrace, Set<IPUtils.ProxyClient> clientTraces) {
        return new ReportClientTraceRunnable(initialize(), namespace, wzbTrace, clientTraces);
    }

    private ElasticsearchRestTemplate initialize() {
        assert wzbTraceStorage != null;
        if (null == NAMESPACE) {
            String namespace = Optional.ofNullable(this.namespace).orElse("").trim();
            if ("".equals(namespace)) {
                namespace = NAMESPACE_DEFAULT;
            }
            NAMESPACE = namespace;
        }
        return this.wzbTraceStorage.getBean(WzbTraceElasticsearchRestTemplate.class).getObject();
    }

    @Getter
    static class TraceRunnable implements Runnable {

        private final ElasticsearchRestTemplate restTemplate;

        private final String namespace;

        private final WzbTrace wzbTrace;

        TraceRunnable(ElasticsearchRestTemplate restTemplate, String namespace, WzbTrace wzbTrace) {
            this.restTemplate = restTemplate;
            this.namespace = namespace;
            this.wzbTrace = wzbTrace;
        }

        @Override
        public void run() {
            try {
                Date now = new Date();
                String index = namespace + COLLECT_INDEX + PARTITION_FORMAT.format(now);
                IndexCoordinates docIndex = IndexCoordinates.of(index);
                restTemplate.save(wzbTrace, docIndex);
                transform(now);
            } catch (Throwable throwable) {
                log.error("trace data report error", throwable);
            }
        }

        protected String getTraceId() {
            String traceId = wzbTrace.getTraceId();
            if (traceId.contains(TraceTools.TRACE_DELIMITER)) {
                traceId = traceId.substring(0, traceId.indexOf(TraceTools.TRACE_DELIMITER));
            }
            return traceId;
        }

        protected int[] findThreshold() {
            String index = getNamespace() + THRESHOLD_CONFIG;
            IndexCoordinates docIndex = IndexCoordinates.of(index);

            ThresholdConfigDoc configDoc = findProjectThreshold(docIndex);
            int threshold = null == configDoc || null == configDoc.getThreshold() ? 10 : configDoc.getThreshold();
            int pathThreshold = findPathThreshold(configDoc);
            return new int[]{threshold, pathThreshold};
        }

        protected void transform(Date executeTime) {

        }

        private ThresholdConfigDoc findProjectThreshold(IndexCoordinates docIndex) {
            ThresholdConfigDoc configDoc = null;
            if (restTemplate.indexOps(docIndex).exists()) {
                NativeSearchQuery configQuery = new NativeSearchQueryBuilder()
                        .withQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery(TraceTools.PROJECT, wzbTrace.getProject())))
                        .build();
                SearchHit<ThresholdConfigDoc> searchHit = restTemplate.searchOne(configQuery, ThresholdConfigDoc.class, docIndex);
                if (null != searchHit) {
                    configDoc = searchHit.getContent();
                }
            }
            return configDoc;
        }

        private int findPathThreshold(ThresholdConfigDoc configDoc) {
            int threshold = 10;
            if (null != configDoc && configDoc.getPathThresholds() != null) {

                Optional<ThresholdConfigDoc.PathThreshold> pathThreshold = configDoc.getPathThresholds().stream()
                        .filter(pathThresholds -> Objects.equals(wzbTrace.getPath(), pathThresholds.getPath())).findFirst();
                if (pathThreshold.isPresent()) {
                    threshold = pathThreshold.get().getPathThreshold();
                }
            }
            return threshold;
        }
    }

    static class ReportTraceRunnable extends TraceRunnable {

        private final String parentProject;

        ReportTraceRunnable(ElasticsearchRestTemplate restTemplate, String namespace, WzbTrace wzbTrace, String parentProject) {
            super(restTemplate, namespace, wzbTrace);
            this.parentProject = parentProject;
        }

        @Override
        protected void transform(Date executeTime) {
            if (!getWzbTrace().isSameUse() && StringUtil.isNotBlank(parentProject)) {
                String traceId = getTraceId();
                int[] thresholds = findThreshold();

                HttpStatus httpStatus = HttpStatus.valueOf(getWzbTrace().getStatus());
                String index = getNamespace() + TRANSFORM_INDEX + PARTITION_FORMAT.format(executeTime);
                IndexCoordinates docIndex = IndexCoordinates.of(index);

                getRestTemplate().save(TraceTransformDoc.builder()
                                .traceId(traceId)
                                .component(parentProject)
                                .sourceComponent(getWzbTrace().getProject())
                                .reqRate(1)
                                .respTime(getWzbTrace().getDuration())
                                .timestamp(getWzbTrace().getTimestamp())
                                .errorRate(httpStatus.is2xxSuccessful() ? 0 : 1)
                                .outRate(1)
                                .outTimeout(getWzbTrace().getDuration())
                                .outErrorRate(httpStatus.is2xxSuccessful() ? 0 : 1)
                                .threshold(thresholds[0])
                                .pathThreshold(thresholds[1])
                                .build(),
                        docIndex);

            }
        }
    }

    static class ReportClientTraceRunnable extends TraceRunnable {

        private final Set<IPUtils.ProxyClient> clientTraces;

        ReportClientTraceRunnable(ElasticsearchRestTemplate restTemplate, String namespace, WzbTrace wzbTrace, Set<IPUtils.ProxyClient> clientTraces) {
            super(restTemplate, namespace, wzbTrace);
            this.clientTraces = clientTraces;
        }

        @Override
        protected void transform(Date executeTime) {
            if (!getWzbTrace().isSameUse()) {
                String traceId = getTraceId();
                int[] thresholds = findThreshold();
                HttpStatus httpStatus = HttpStatus.valueOf(getWzbTrace().getStatus());
                String index = getNamespace() + TRANSFORM_INDEX + PARTITION_FORMAT.format(executeTime);
                IndexCoordinates docIndex = IndexCoordinates.of(index);

                List<TraceTransformDoc> docList = new ArrayList<>();

                IPUtils.ProxyClient[] proxyClients = this.clientTraces.toArray(new IPUtils.ProxyClient[0]);
                IPUtils.ProxyClient currentClient;
                for (int i = 1; i < proxyClients.length; i++) {
                    IPUtils.ProxyClient proxyClient = proxyClients[i - 1];
                    currentClient = proxyClients[i];
                    docList.add(generateClientTrace(traceId, null, httpStatus, proxyClient, currentClient.getComponent()));
                }
                currentClient = proxyClients[proxyClients.length - 1];
                docList.add(generateClientTrace(traceId, thresholds, httpStatus, currentClient, getWzbTrace().getProject()));
                getRestTemplate().save(docList, docIndex);
            }
        }

        private TraceTransformDoc generateClientTrace(String traceId,
                                                      int[] thresholds,
                                                      HttpStatus httpStatus,
                                                      IPUtils.ProxyClient proxyClient,
                                                      String sourceComponent) {
            return TraceTransformDoc.builder()
                    .traceId(traceId)
                    .component(proxyClient.getComponent())
                    .sourceComponent(sourceComponent)
                    .reqRate(1)
                    .respTime(getWzbTrace().getDuration())
                    .timestamp(getWzbTrace().getTimestamp())
                    .errorRate(httpStatus.is2xxSuccessful() ? 0 : 1)
                    .outRate(1)
                    .outTimeout(getWzbTrace().getDuration())
                    .outErrorRate(httpStatus.is2xxSuccessful() ? 0 : 1)
                    .threshold(null == thresholds ? Integer.MAX_VALUE : thresholds[0])
                    .pathThreshold(null == thresholds ? Integer.MAX_VALUE : thresholds[1])
                    .build();
        }
    }

    @Getter
    @Setter
    public static class Rest implements Serializable {
        private String[] uris;
        private String username;
        private String password;
        private Duration connectionTimeout;
        private Duration readTimeout;
    }

    public static class WzbTraceElasticsearchRestTemplate {

        private final ElasticsearchRestTemplate elasticsearchRestTemplate;

        public WzbTraceElasticsearchRestTemplate(RestHighLevelClient client, ElasticsearchConverter elasticsearchConverter) {
            this.elasticsearchRestTemplate = new ElasticsearchRestTemplate(client, elasticsearchConverter);
        }

        public ElasticsearchRestTemplate getObject() {
            return this.elasticsearchRestTemplate;
        }
    }
}
