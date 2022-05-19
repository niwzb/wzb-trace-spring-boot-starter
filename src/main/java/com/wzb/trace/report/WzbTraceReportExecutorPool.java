package com.wzb.trace.report;

import com.wzb.trace.threadpool.WzbExecutorPool;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class WzbTraceReportExecutorPool {

    public static WzbExecutorPool.WzbExecutor getExecutor() {
        return WzbExecutorPool.newExecutor("WzbTraceReport", 10, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(1024), new ThreadPoolExecutor.DiscardPolicy());
    }

}
