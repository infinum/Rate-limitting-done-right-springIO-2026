package com.infinum.ratelimiting.service;

import java.time.Duration;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Service;

import com.infinum.ratelimiting.config.RateLimitProperties;
import com.infinum.ratelimiting.config.RateLimitProperties.BucketConfig;

@Service
public class RateLimitService {

    private static final String COMMUNITY_POOL_KEY = "community-pool";

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties properties;
    private final BurstEvaluator burstEvaluator;

    public RateLimitService(ProxyManager<String> proxyManager, RateLimitProperties properties,
            BurstEvaluator burstEvaluator) {
        this.proxyManager = proxyManager;
        this.properties = properties;
        this.burstEvaluator = burstEvaluator;
    }

    public RateLimitResult tryConsume(String tenantId) {
        BucketConfig primaryConfig = resolvePrimaryConfig(tenantId);

        Bucket primary = proxyManager.getProxy(tenantId, () -> toBucketConfiguration(primaryConfig));
        ConsumptionProbe probe = primary.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return new RateLimitResult(probe, RateLimitSource.PRIMARY);
        }

        BucketConfig communityConfig = properties.communityPool();
        Bucket community = proxyManager.getProxy(COMMUNITY_POOL_KEY, () -> toCommunityBucketConfiguration(communityConfig));

        BurstDecision decision = burstEvaluator.calculateCanUserBurstIn(tenantId);

        if (decision.allowed()) {
            ConsumptionProbe communityProbe = community.tryConsumeAndReturnRemaining(decision.tokens());
            if (communityProbe.isConsumed()) {
                return new RateLimitResult(communityProbe, RateLimitSource.COMMUNITY);
            }
        }

        return new RateLimitResult(probe, RateLimitSource.PRIMARY);
    }

    public void returnCommunityTokens(int tokens) {
        BucketConfig communityConfig = properties.communityPool();
        Bucket community = proxyManager.getProxy(COMMUNITY_POOL_KEY, () -> toCommunityBucketConfiguration(communityConfig));
        community.addTokens(tokens);
    }

    private BucketConfig resolvePrimaryConfig(String tenantId) {
        RateLimitProperties.TenantConfig tenant = properties.tenants().get(tenantId);
        if (tenant == null) {
            throw new IllegalArgumentException("No configuration found for tenant: " + tenantId);
        }
        return tenant.primary();
    }

    private BucketConfiguration toBucketConfiguration(BucketConfig config) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(config.capacity())
                        .refillIntervally(config.refillTokens(), config.refillDuration()))
                .build();
    }

    private BucketConfiguration toCommunityBucketConfiguration(BucketConfig config) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(config.capacity())
                        .refillIntervally(config.refillTokens(), config.refillDuration()))
                .build();
    }
}
