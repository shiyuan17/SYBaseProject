package com.company.bl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "com.company")
@EnableCaching
public class BlCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlCenterApplication.class, args);
    }
}
