package com.wzb.trace.aspect.configure;

import com.wzb.trace.aspect.RedisTemplateAspect;
import com.wzb.trace.report.WzbTraceStorage;
import org.springframework.context.annotation.Bean;

public class RedisTemplateAspectConfigure {

    @Bean
    public RedisTemplateAspect redisTemplateAspect(WzbTraceStorage wzbTraceStorage) {
        return new RedisTemplateAspect(wzbTraceStorage);
    }
}
