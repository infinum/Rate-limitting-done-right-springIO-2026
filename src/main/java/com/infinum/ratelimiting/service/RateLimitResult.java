package com.infinum.ratelimiting.service;

import io.github.bucket4j.ConsumptionProbe;

public record RateLimitResult(ConsumptionProbe probe, RateLimitSource source) {

    public boolean isConsumed() {
        return probe.isConsumed();
    }
}