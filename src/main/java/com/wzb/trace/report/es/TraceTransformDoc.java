package com.wzb.trace.report.es;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

@Setter
@Getter
@Builder
public class TraceTransformDoc implements Serializable {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "trace-id")
    private String traceId;

    @Field(type = FieldType.Keyword, name = "@timestamp")
    private String timestamp;

    @Field(type = FieldType.Keyword, name = "component")
    private String component;

    @Field(type = FieldType.Keyword, name = "source-component")
    private String sourceComponent;

    /**
     * @Field(type = FieldType.Keyword, name = "target-component")
     * private String targetComponent;
     * @Field(type = FieldType.Keyword, name = "external-origin")
     * private String externalOrigin;
     * @Field(type = FieldType.Keyword, name = "external-target")
     * private String externalTarget;
     */

    @Field(type = FieldType.Integer, name = "type")
    private int type;

    @Field(type = FieldType.Long, name = "resp-time")
    private long respTime;

    @Field(type = FieldType.Integer, name = "req-rate")
    private int reqRate;

    @Field(type = FieldType.Integer, name = "error-rate")
    private int errorRate;

    @Field(type = FieldType.Integer, name = "threshold")
    private int threshold;

    @Field(type = FieldType.Integer, name = "path-threshold")
    private int pathThreshold;

    @Field(type = FieldType.Long, name = "out-timeout")
    private long outTimeout;

    @Field(type = FieldType.Integer, name = "out-error-rate")
    private int outErrorRate;

    @Field(type = FieldType.Integer, name = "out-rate")
    private int outRate;
}
