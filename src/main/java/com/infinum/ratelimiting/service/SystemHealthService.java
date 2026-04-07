package com.infinum.ratelimiting.service;

import org.springframework.stereotype.Service;

@Service
public class SystemHealthService {

    private volatile boolean healthy = true;

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }
}