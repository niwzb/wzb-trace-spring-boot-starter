package com.wzb.trace.configure;

import com.wzb.trace.report.storage.WzbTraceEsStorageReport;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WzbTraceStorageConfiguration {

    private String storage = "elasticsearch";

    private Boolean enable = true;

    private WzbTraceEsStorageReport elasticsearch;
}
