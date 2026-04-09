package com.infinum.ratelimiting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.infinum.ratelimiting.config.RateLimitProperties;

@SpringBootApplication
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitingApplication {

    public static void main(String[] args) {
        SpringApplication.run(RateLimitingApplication.class, args);
    }
}
