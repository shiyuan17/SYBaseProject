package com.company.common.web.config;

import com.company.common.web.response.ApiResponseReturnValueHandler;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CommonWebConfiguration {

    @org.springframework.context.annotation.Bean
    public static BeanPostProcessor apiResponseReturnValueHandlerConfigurer(
        ObjectProvider<List<HttpMessageConverter<?>>> messageConvertersProvider) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RequestMappingHandlerAdapter adapter) {
                    List<HttpMessageConverter<?>> messageConverters = messageConvertersProvider.getIfAvailable(List::of);
                    List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
                    handlers.add(new ApiResponseReturnValueHandler(messageConverters));
                    handlers.addAll(adapter.getReturnValueHandlers());
                    adapter.setReturnValueHandlers(handlers);
                }
                return bean;
            }
        };
    }
}
