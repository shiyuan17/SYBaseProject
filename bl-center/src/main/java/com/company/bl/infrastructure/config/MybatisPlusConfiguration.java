package com.company.bl.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test-no-db")
@MapperScan("com.company.bl.infrastructure.persistence")
public class MybatisPlusConfiguration {
}
