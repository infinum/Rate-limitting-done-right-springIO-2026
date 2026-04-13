package com.infinum.ratelimiting.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.infinum.ratelimiting.config.RateLimitProperties;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private static final String DEFAULT_TIER = "STANDARD";

    private final ProxyManager<String> proxyManager;
    private final Map<String, Tier> tiers;
    private final ConcurrentHashMap<String, String> tenantTiers;

    public RateLimitService(ProxyManager<String> proxyManager, RateLimitProperties properties) {
        this.proxyManager = proxyManager;
        this.tiers = properties.tiers().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Tier.from(e.getValue())));
        this.tenantTiers = new ConcurrentHashMap<>(properties.tenantTiers());
    }

    public ConsumptionProbe tryConsumeAndReturnRemaining(String tenantId) {
        Bucket bucket = resolveBucket(tenantId);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public void changeTier(String tenantId, String tierName) {
        Tier tier = tiers.get(tierName);
        if (tier == null) {
            throw new IllegalArgumentException("Unknown tier: " + tierName);
        }
        Bucket bucket = resolveBucket(tenantId);
        bucket.replaceConfiguration(tier.bucketConfiguration().get(), TokensInheritanceStrategy.AS_IS);
        tenantTiers.put(tenantId, tierName);
        log.info("Tenant '{}' moved to tier '{}', bucket configuration replaced", tenantId, tierName);
    }

    private Bucket resolveBucket(String tenantId) {
        String tierName = tenantTiers.getOrDefault(tenantId, DEFAULT_TIER);
        Tier tier = tiers.get(tierName);
        if (tier == null) {
            throw new IllegalArgumentException("No tier configuration found for tier: " + tierName);
        }
        return proxyManager.getProxy(tenantId, tier.bucketConfiguration());
    }
}
