package com.infinum.ratelimiting.config;

import io.github.bucket4j.BucketConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "rate-limiting")
public record RateLimitProperties(
        Map<String, String> tenants,
        Map<String, TierProperties> tiers
) {
    public record TierProperties(List<LimitProperties> limits) {

        public BucketConfiguration toBucketConfiguration() {
            var builder = BucketConfiguration.builder();
            limits.forEach(l -> builder.addLimit(limit ->
                    limit.capacity(l.capacity()).refillGreedy(l.refillTokens(), l.refillDuration())));
            return builder.build();
        }
    }

    public record LimitProperties(long capacity, long refillTokens, Duration refillDuration) {}
}
