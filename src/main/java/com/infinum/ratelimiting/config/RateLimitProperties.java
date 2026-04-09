package com.infinum.ratelimiting.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limiting")
public record RateLimitProperties(Map<String, TierProperties> tiers, Map<String, String> tenantTiers) {

    public record TierProperties(List<LimitProperties> limits) {
    }

    public record LimitProperties(long capacity, long refillTokens, Duration refillDuration) {
    }
}
