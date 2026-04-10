package com.infinum.ratelimiting.service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final ProxyManager<String> proxyManager;

    private final ConcurrentHashMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    public RateLimitService(ProxyManager<String> proxyManager) {this.proxyManager = proxyManager;}

    public record Result(ConsumptionProbe probe, String source) {}

    public Result tryConsumeAndReturnRemaining(String tenantId) {
        try {
            return new Result(tryConsumeAndReturnRemainingFromRedis(tenantId), "redis");
        } catch (RedisException e) {
            log.warn("Redis unavailable, falling back to in-memory rate limiting for tenant: {}", tenantId);
            return new Result(tryConsumeAndReturnRemainingFromLocal(tenantId), "local");
        }
    }

    private ConsumptionProbe tryConsumeAndReturnRemainingFromRedis(String tenantId) {
        Bucket bucket = resolveRedisBucket(tenantId);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket resolveRedisBucket(String tenantId) {
        return proxyManager.getProxy(
                tenantId, () -> BucketConfiguration.builder()
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
                        .addLimit(limit -> limit.capacity(100).refillIntervally(100, Duration.ofHours(1)))
                        .build());
    }

    private ConsumptionProbe tryConsumeAndReturnRemainingFromLocal(String tenantId) {
        Bucket bucket = resolveLocalBucket(tenantId);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket resolveLocalBucket(String tenantId) {
        return localBuckets.computeIfAbsent(
                tenantId,
                id -> Bucket.builder()
                        .addLimit(limit -> limit.capacity(1).refillGreedy(1, Duration.ofSeconds(1)))
                        .addLimit(limit -> limit.capacity(10).refillIntervally(10, Duration.ofHours(1)))
                        .build());
    }
}
