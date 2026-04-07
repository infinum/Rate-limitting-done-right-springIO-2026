package com.infinum.ratelimiting.service;

import java.time.Duration;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final ProxyManager<String> proxyManager;

    public RateLimitService(ProxyManager<String> proxyManager) {this.proxyManager = proxyManager;}

    public ConsumptionProbe tryConsumeAndReturnRemaining(String tenantId) {
        Bucket bucket = resolveBucket(tenantId);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket resolveBucket(String tenantId) {
        return proxyManager.getProxy(
                tenantId, () -> BucketConfiguration.builder()
                        .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1)))
                        .build());
    }
}
