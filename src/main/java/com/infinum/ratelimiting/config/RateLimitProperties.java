package com.infinum.ratelimiting.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limiting")
public record RateLimitProperties(BucketConfig communityPool, Map<String, TenantConfig> tenants) {

    public record TenantConfig(BucketConfig primary) {
    }

    public record BucketConfig(long capacity, long refillTokens, Duration refillDuration) {
    }
}
