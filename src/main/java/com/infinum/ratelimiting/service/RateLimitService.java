package com.infinum.ratelimiting.service;

import com.infinum.ratelimiting.config.RateLimitProperties;
import com.infinum.ratelimiting.config.RateLimitProperties.TierProperties;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private static final String DEFAULT_TIER = "STANDARD";

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties properties;

    public RateLimitService(ProxyManager<String> proxyManager, RateLimitProperties properties) {
        this.proxyManager = proxyManager;
        this.properties = properties;
    }

    public ConsumptionProbe tryConsumeAndReturnRemaining(String tenantId) {
        Bucket bucket = resolveBucket(tenantId);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket resolveBucket(String tenantId) {
        String tierName = properties.tenants().getOrDefault(tenantId, DEFAULT_TIER);
        TierProperties tier = resolveTier(tierName);
        return proxyManager.getProxy(tenantId, tier::toBucketConfiguration);
    }

    private TierProperties resolveTier(String tierName) {
        TierProperties tier = properties.tiers().get(tierName);
        if (tier == null) {
            throw new IllegalArgumentException("No tier configuration found for tier: " + tierName);
        }
        return tier;
    }
}
