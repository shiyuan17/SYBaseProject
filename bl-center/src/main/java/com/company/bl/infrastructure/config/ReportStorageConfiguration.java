package com.company.bl.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(ReportStorageProperties.class)
public class ReportStorageConfiguration {

    @Bean(name = "reportOfdRepairExecutor")
    public ThreadPoolTaskExecutor reportOfdRepairExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("report-ofd-repair-");
        executor.initialize();
        return executor;
    }
}
