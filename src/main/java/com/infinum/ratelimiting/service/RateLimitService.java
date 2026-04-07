package com.infinum.ratelimiting.service;

import java.util.Map;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private static final Map<String, Tier> TENANT_TIERS = Map.of(
            "spring-shop", Tier.STANDARD,
            "spring-bank", Tier.PREMIUM
    );

    private final ProxyManager<String> proxyManager;

    public RateLimitService(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    public ConsumptionProbe tryConsumeAndReturnRemaining(String tenantId) {
        Bucket bucket = resolveBucket(tenantId);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket resolveBucket(String tenantId) {
        Tier tier = TENANT_TIERS.getOrDefault(tenantId, Tier.STANDARD);
        return proxyManager.getProxy(tenantId, tier.bucketConfiguration());
    }
}