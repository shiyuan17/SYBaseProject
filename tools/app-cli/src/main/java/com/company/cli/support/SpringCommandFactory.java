package com.company.cli.support;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
public class SpringCommandFactory implements CommandLine.IFactory {

    private final ApplicationContext applicationContext;

    public SpringCommandFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        try {
            return applicationContext.getBean(cls);
        } catch (NoSuchBeanDefinitionException exception) {
            AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
            return beanFactory.createBean(cls);
        }
    }
}

