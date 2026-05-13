package com.company.user.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@Configuration
public class NoOpTransactionManager {

    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
                // 当前脚手架未接入真实事务资源，这里仅保留应用层事务边界入口。
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                // no-op
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
                // no-op
            }
        };
    }
}
