package com.wzb.trace.report;

import com.wzb.trace.network.TraceTools;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.function.Function;


@Builder
@Setter
@Getter
public class WzbTrace {

    @Id
    private String id;

    @Field(name = TraceTools.X_TRACE_ID)
    private String traceId;

    @Field(name = TraceTools.X_TRACE_LENGTH)
    private int traceLength;

    @Field(name = TraceTools.X_REQUEST_DURATION)
    private long duration;

    @Field(name = TraceTools.X_REQUEST_SIZE)
    private long requestSize;

    @Field(name = TraceTools.X_RESPONSE_SIZE)
    private long responseSize;

    @Field(name = TraceTools.X_CLIENT_REAL_IP)
    private String clientIP;

    @Field(name = TraceTools.X_REQUEST_PATH)
    private String path;

    @Field(name = TraceTools.X_RESPONSE_STATUS)
    private int status;

    @Field(name = TraceTools.TIME_STAMP)
    private String timestamp;

    @Field(name = TraceTools.PROJECT)
    private String project;

    @Field(name = TraceTools.TRACE_SAME_USE)
    private boolean sameUse;

    public <R> R transform(Function<WzbTrace, R> traceFunction) {
        return traceFunction.apply(this);
    }
}
