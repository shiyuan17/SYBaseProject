package com.company.bl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.company")
public class BlCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlCenterApplication.class, args);
    }
}
