package com.wzb.trace.report;


import com.wzb.trace.network.TraceTools;
import com.wzb.trace.utils.DateUtil;
import com.wzb.trace.utils.IPUtils;

import java.util.Date;
import java.util.Set;

public interface WzbTraceReport {

    default boolean report(WzbTrace wzbTrace) {
        return this.report(wzbTrace, (String) null);
    }

    default boolean report(WzbTrace wzbTrace, String parentProject) {
        wzbTrace.setTraceLength(wzbTrace.getTraceId().length());
        WzbTraceReportExecutorPool.getExecutor().execute(reportRunnable(wzbTrace, parentProject));
        recordSelf();
        return true;
    }

    default boolean report(WzbTrace wzbTrace, Set<IPUtils.ProxyClient> clientTraces) {
        wzbTrace.setTraceLength(wzbTrace.getTraceId().length());
        WzbTraceReportExecutorPool.getExecutor().execute(reportRunnable(wzbTrace, clientTraces));
        recordSelf();
        return true;
    }

    default Runnable reportRunnable(WzbTrace wzbTrace, String parentProject) {
        return () -> {};
    }

    default Runnable reportRunnable(WzbTrace wzbTrace, Set<IPUtils.ProxyClient> clientTraces) {
        return () -> {};
    }

    default void recordSelf() {
        if (null == TraceTools.get(TraceTools.ES_FLAG)) {
            TraceTools.put(TraceTools.ES_FLAG, TraceTools.get(TraceTools.X_TRACE_ID));
            WzbTraceReportExecutorPool.getExecutor().execute(reportRunnable(WzbTrace.builder()
                    .clientIP(TraceTools.getLocalIp())
                    .project(TraceTools.ES_FLAG)
                    .duration(3)
                    .requestSize(0)
                    .responseSize(0)
                    .traceId(TraceTools.get(TraceTools.X_TRACE_ID) + "|" + TraceTools.getTraceId())
                    .path("")
                    .status(200)
                    .timestamp(DateUtil.dateFormat(new Date(), DateUtil.UTC_DATE_FORMAT, DateUtil.TIMEZONE_UTC))
                    .build(), TraceTools.get(TraceTools.PROJECT)));
        }
    }
}
