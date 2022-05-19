package com.wzb.trace.configure;

import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

@DependsOn({"elasticsearchConverter"})
@Import(WzbTraceConfigureDefinitionRegistrar.class)
public class WzbTraceAutoConfiguration {
}
