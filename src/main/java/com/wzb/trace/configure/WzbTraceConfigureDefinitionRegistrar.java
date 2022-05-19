package com.wzb.trace.configure;

import com.wzb.trace.utils.SpringContextUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;

public class WzbTraceConfigureDefinitionRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
        BeanDefinitionBuilder wzbTraceConfigureBeanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(WzbTraceConfigure.class);
        defaultListableBeanFactory.registerBeanDefinition("wzbTraceConfigure", wzbTraceConfigureBeanDefinitionBuilder.getBeanDefinition());

        BeanDefinitionBuilder springContextUtilAwareBeanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(SpringContextUtil.SpringContextUtilAware.class);
        defaultListableBeanFactory.registerBeanDefinition("springContextUtilAware", springContextUtilAwareBeanDefinitionBuilder.getBeanDefinition());

        BeanDefinitionBuilder wzbTraceLoadPluginsRegistrarBeanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(WzbTraceLoadPluginsRegistrar.class);
        defaultListableBeanFactory.registerBeanDefinition("wzbTraceLoadPluginsRegistrar", wzbTraceLoadPluginsRegistrarBeanDefinitionBuilder.getBeanDefinition());

    }

}
