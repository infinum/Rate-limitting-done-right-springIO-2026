package com.infinum.ratelimiting.service;

import java.util.function.Supplier;

import io.github.bucket4j.BucketConfiguration;

import com.infinum.ratelimiting.config.RateLimitProperties;

public record Tier(Supplier<BucketConfiguration> bucketConfiguration) {

    public static Tier from(RateLimitProperties.TierProperties props) {
        return new Tier(() -> {
            var builder = BucketConfiguration.builder();
            for (var limit : props.limits()) {
                builder.addLimit(l -> l
                        .capacity(limit.capacity())
                        .refillGreedy(limit.refillTokens(), limit.refillDuration()));
            }
            return builder.build();
        });
    }
}
