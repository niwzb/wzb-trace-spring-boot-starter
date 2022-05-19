package com.wzb.trace.report.storage;

import com.wzb.trace.report.WzbTrace;
import com.wzb.trace.report.WzbTraceReport;
import com.wzb.trace.utils.IPUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class WzbTraceRejectReport implements WzbTraceReport {

    @Override
    public boolean report(WzbTrace wzbTrace, String parentProject) {
        log.info("trace data reject report");
        return true;
    }

    @Override
    public boolean report(WzbTrace wzbTrace, Set<IPUtils.ProxyClient> clientTraces) {
        log.info("trace data reject report");
        return true;
    }
}
