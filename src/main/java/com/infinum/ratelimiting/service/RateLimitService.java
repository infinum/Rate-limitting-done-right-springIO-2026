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

    private final SystemHealthService systemHealthService;

    public RateLimitService(ProxyManager<String> proxyManager, SystemHealthService systemHealthService) {
        this.proxyManager = proxyManager;
        this.systemHealthService = systemHealthService;
    }

    public RateLimitResult tryConsume(String tenantId) {
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
}