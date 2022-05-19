package com.wzb.trace.aspect.configure;

import com.wzb.trace.aspect.JdbcTemplateAspect;
import com.wzb.trace.report.WzbTraceStorage;
import org.springframework.context.annotation.Bean;

public class JdbcTemplateAspectConfigure {

    @Bean
    public JdbcTemplateAspect jdbcTemplateAspect(WzbTraceStorage wzbTraceStorage) {
        return new JdbcTemplateAspect(wzbTraceStorage);
    }

}
