package com.infinum.ratelimiting.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private static final Map<String, Tier> TENANT_TIERS = Map.of(
            "spring-shop", Tier.STANDARD,
            "spring-bank", Tier.PREMIUM
    );

    private final ProxyManager<String> proxyManager;

    private final SystemHealthService systemHealthService;

    private final ConcurrentHashMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    public RateLimitService(ProxyManager<String> proxyManager, SystemHealthService systemHealthService) {
        this.proxyManager = proxyManager;
        this.systemHealthService = systemHealthService;
    }

    public RateLimitResult tryConsume(String tenantId) {
        try {
            return tryConsumeFromRedis(tenantId);
        } catch (RedisException e) {
            log.warn("Redis unavailable, falling back to in-memory rate limiting for tenant: {}", tenantId);
            return tryConsumeFromLocalBucket(tenantId);
        }
    }

    private RateLimitResult tryConsumeFromRedis(String tenantId) {
        Tier tier = TENANT_TIERS.getOrDefault(tenantId, Tier.STANDARD);
        Bucket primary = proxyManager.getProxy(tenantId, tier.primaryConfig());
        ConsumptionProbe probe = primary.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return new RateLimitResult(probe, false);
        }

        if (systemHealthService.isHealthy()) {
            Bucket burst = proxyManager.getProxy(tier.burstPoolKey(tenantId), tier.burstConfig());
            ConsumptionProbe burstProbe = burst.tryConsumeAndReturnRemaining(1);
            if (burstProbe.isConsumed()) {
                return new RateLimitResult(burstProbe, true);
            }
        }

        return new RateLimitResult(probe, false);
    }

    private RateLimitResult tryConsumeFromLocalBucket(String tenantId) {
        Tier tier = TENANT_TIERS.getOrDefault(tenantId, Tier.STANDARD);
        Bucket local = localBuckets.computeIfAbsent(
                tenantId,
                key -> tier.buildLocalBucket());
        return new RateLimitResult(local.tryConsumeAndReturnRemaining(1), false);
    }
}