package com.wzb.trace.aspect;

import com.wzb.trace.report.WzbTrace;
import com.wzb.trace.report.WzbTraceStorage;

public interface ClassAspect {

    static boolean exists(String clazz) {
        return loadClass(clazz) != null;
    }

    static Class<?> loadClass(String clazz) {
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException ignored) {}
        return null;
    }

    WzbTraceStorage getWzbTraceStorage();

    default void report(WzbTrace wzbTrace) {
        getWzbTraceStorage().getWzbTraceStorageReport().report(wzbTrace, getWzbTraceStorage().getProject());
    }
}
