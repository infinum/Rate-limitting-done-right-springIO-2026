package com.infinum.ratelimiting.service;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.local.LocalBucketBuilder;

public enum Tier {

    STANDARD(
            () -> BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(10)))
                    .addLimit(limit -> limit.capacity(1000).refillGreedy(1000, Duration.ofHours(1)))
                    .build(),
            () -> BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(20).refillGreedy(20, Duration.ofSeconds(30)))
                    .build(),
            builder -> builder.addLimit(limit -> limit.capacity(1).refillGreedy(1, Duration.ofSeconds(10)))
    ),

    PREMIUM(
            () -> BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofSeconds(10)))
                    .addLimit(limit -> limit.capacity(10000).refillGreedy(10000, Duration.ofHours(1)))
                    .build(),
            () -> BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(200).refillGreedy(200, Duration.ofSeconds(30)))
                    .build(),
            builder -> builder.addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(10)))
    );

    private final Supplier<BucketConfiguration> primaryConfig;

    private final Supplier<BucketConfiguration> burstConfig;

    private final Consumer<LocalBucketBuilder> localConfig;

    Tier(
            Supplier<BucketConfiguration> primaryConfig,
            Supplier<BucketConfiguration> burstConfig,
            Consumer<LocalBucketBuilder> localConfig) {
        this.primaryConfig = primaryConfig;
        this.burstConfig = burstConfig;
        this.localConfig = localConfig;
    }

    public Supplier<BucketConfiguration> primaryConfig() {
        return primaryConfig;
    }

    public Supplier<BucketConfiguration> burstConfig() {
        return burstConfig;
    }

    public Bucket buildLocalBucket() {
        LocalBucketBuilder builder = Bucket.builder();
        localConfig.accept(builder);
        return builder.build();
    }

    public String burstPoolKey(String tenantId) {
        return "burst-pool:" + tenantId;
    }
}