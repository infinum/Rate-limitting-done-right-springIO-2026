package com.infinum.ratelimiting.service;

import io.github.bucket4j.ConsumptionProbe;

public record RateLimitResult(ConsumptionProbe probe, boolean fromBurst) {

    public boolean isConsumed() {
        return probe.isConsumed();
    }
}