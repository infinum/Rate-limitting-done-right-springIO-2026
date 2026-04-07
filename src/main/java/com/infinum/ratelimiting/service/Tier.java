package com.infinum.ratelimiting.service;

import java.time.Duration;
import java.util.function.Supplier;

import io.github.bucket4j.BucketConfiguration;

public enum Tier {

    STANDARD(
            () -> BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(10)))
                    .addLimit(limit -> limit.capacity(1000).refillGreedy(1000, Duration.ofHours(1)))
                    .build(),
            () -> BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(20).refillGreedy(20, Duration.ofSeconds(30)))
                    .build()
    ),

    PREMIUM(
            () -> BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofSeconds(10)))
                    .addLimit(limit -> limit.capacity(10000).refillGreedy(10000, Duration.ofHours(1)))
                    .build(),
            () -> BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(200).refillGreedy(200, Duration.ofSeconds(30)))
                    .build()
    );

    private final Supplier<BucketConfiguration> primaryConfig;

    private final Supplier<BucketConfiguration> burstConfig;

    Tier(Supplier<BucketConfiguration> primaryConfig, Supplier<BucketConfiguration> burstConfig) {
        this.primaryConfig = primaryConfig;
        this.burstConfig = burstConfig;
    }

    public Supplier<BucketConfiguration> primaryConfig() {
        return primaryConfig;
    }

    public Supplier<BucketConfiguration> burstConfig() {
        return burstConfig;
    }

    public String burstPoolKey(String tenantId) {
        return "burst-pool:" + tenantId;
    }
}