package com.wzb.trace.configure;

import com.wzb.trace.aspect.ClassAspect;
import com.wzb.trace.aspect.JpaPropertiesAspect;
import com.wzb.trace.aspect.configure.JdbcTemplateAspectConfigure;
import com.wzb.trace.aspect.configure.RedisTemplateAspectConfigure;
import com.wzb.trace.cglib.JpaInterceptorCreator;
import com.wzb.trace.cglib.MybatisInterceptorCreator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class WzbTraceLoadPluginsRegistrar implements BeanFactoryAware, EnvironmentAware {

    private BeanFactory beanFactory;

    private Environment environment;

    private static volatile boolean READY = false;

    private static final Set<String> envSet = new HashSet<String>() {{
        add("dev");
        add("develop");
        add("test");
        add("staging");
        add("prod");
        add("hotfix");
        add("master");
    }};

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        if (!READY && beanFactory != null) {
            READY = true;
            loadPlugins();
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        if (!READY && environment != null) {
            READY = true;
            loadPlugins();
        }
    }

    private void loadPlugins() {

        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;

        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0 || Stream.of(activeProfiles).anyMatch(envSet::contains)) {
            if (ClassAspect.exists("org.springframework.data.redis.core.RedisTemplate")) {
                BeanDefinitionBuilder redisTemplateAspectBeanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(RedisTemplateAspectConfigure.class);
                defaultListableBeanFactory.registerBeanDefinition("redisTemplateAspectConfigure", redisTemplateAspectBeanDefinitionBuilder.getBeanDefinition());
            }

            if (ClassAspect.exists("org.springframework.jdbc.core.JdbcTemplate")) {
                BeanDefinitionBuilder jdbcTemplateAspectBeanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(JdbcTemplateAspectConfigure.class);
                defaultListableBeanFactory.registerBeanDefinition("jdbcTemplateAspectConfigure", jdbcTemplateAspectBeanDefinitionBuilder.getBeanDefinition());
            }

            if (ClassAspect.exists("org.apache.ibatis.plugin.Interceptor")) {

                Class<?> mybatisInterceptor = MybatisInterceptorCreator.loadMybatisInterceptor();
                if (null != mybatisInterceptor) {
                    BeanDefinitionBuilder mybatisInterceptorBeanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(mybatisInterceptor);
                    defaultListableBeanFactory.registerBeanDefinition("wzbMybatisTraceInterceptor", mybatisInterceptorBeanDefinitionBuilder.getBeanDefinition());
                }
            }

            if (ClassAspect.exists("org.hibernate.resource.jdbc.spi.StatementInspector")) {
                Class<?> wzbJpaTraceInterceptor = JpaInterceptorCreator.loadJpaInterceptor();
                if (null != wzbJpaTraceInterceptor) {
                    BeanDefinitionBuilder wzbJpaPropertiesAspectBeanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(JpaPropertiesAspect.class);
                    wzbJpaPropertiesAspectBeanDefinitionBuilder.addConstructorArgValue(wzbJpaTraceInterceptor);
                    defaultListableBeanFactory.registerBeanDefinition("wzbJpaPropertiesAspect", wzbJpaPropertiesAspectBeanDefinitionBuilder.getBeanDefinition());

                }
            }
        }
    }
}
