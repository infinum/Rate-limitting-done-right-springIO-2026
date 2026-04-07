package com.infinum.ratelimiting.service;

import java.time.Duration;
import java.util.function.Supplier;

import io.github.bucket4j.BucketConfiguration;

public enum Tier {

    STANDARD(() -> BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(10)))
            .addLimit(limit -> limit.capacity(1000).refillGreedy(1000, Duration.ofHours(1)))
            .build()),

    PREMIUM(() -> BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofSeconds(10)))
            .addLimit(limit -> limit.capacity(10000).refillGreedy(10000, Duration.ofHours(1)))
            .build());

    private final Supplier<BucketConfiguration> configSupplier;

    Tier(Supplier<BucketConfiguration> configSupplier) {
        this.configSupplier = configSupplier;
    }

    public Supplier<BucketConfiguration> bucketConfiguration() {
        return configSupplier;
    }
}