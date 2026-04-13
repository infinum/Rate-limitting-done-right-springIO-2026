package com.infinum.ratelimiting.service;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BurstEvaluator {

    private static final Logger log = LoggerFactory.getLogger(BurstEvaluator.class);
    private static final int MAX_BURSTS = 20;
    private static final long COOLDOWN_MINUTES = 5;

    private final ConcurrentMap<String, TenantBurstState> tenantStates = new ConcurrentHashMap<>();
    private final Clock clock;

    public BurstEvaluator(Clock clock) {
        this.clock = clock;
    }

    // Simple algorithm. Works only for 1 instance. Since this is use case dependant you implement your own.
    // In prod env this should happen in LUA script.
    public BurstDecision calculateCanUserBurstIn(String tenantId) {
        TenantBurstState state = tenantStates.computeIfAbsent(tenantId, k -> new TenantBurstState());

        synchronized (state) {
            if (state.blockedUntil != null && clock.instant().isBefore(state.blockedUntil)) {
                log.info("Tenant {} is in cooldown until {}", tenantId, state.blockedUntil);
                return new BurstDecision(false, 0);
            }

            if (state.blockedUntil != null) {
                log.info("Tenant {} cooldown expired, resetting burst count", tenantId);
                state.count = 0;
                state.blockedUntil = null;
            }

            state.count++;

            if (state.count > MAX_BURSTS) {
                state.blockedUntil = clock.instant().plusSeconds(COOLDOWN_MINUTES * 60);
                log.info("Tenant {} exceeded {} bursts, blocked until {}", tenantId, MAX_BURSTS, state.blockedUntil);
                return new BurstDecision(false, 0);
            }

            return new BurstDecision(true, 1);
        }
    }

    private static class TenantBurstState {
        int count;
        Instant blockedUntil;
    }
}
