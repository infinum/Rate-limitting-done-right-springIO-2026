package com.infinum.ratelimiting.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LeakyBucketService {

    private static final Logger log = LoggerFactory.getLogger(LeakyBucketService.class);

    private final StatefulRedisConnection<String, String> connection;
    private final String luaScript;
    private final int capacity;
    private final int leakRate;

    public LeakyBucketService(
            StatefulRedisConnection<String, String> connection,
            @Value("${leaky-bucket.capacity:10}") int capacity,
            @Value("${leaky-bucket.leak-rate:2}") int leakRate) {
        this.connection = connection;
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.luaScript = loadScript();
    }

    public LeakyBucketResult tryConsume(String tenantId) {
        String key = "leaky:" + tenantId;
        long now = System.currentTimeMillis();

        List<Long> result = connection.sync().eval(
                luaScript,
                ScriptOutputType.MULTI,
                new String[]{key},
                String.valueOf(capacity),
                String.valueOf(leakRate),
                String.valueOf(now));

        boolean allowed = result.get(0) == 1;
        long value = result.get(1);

        log.info("Leaky bucket [{}]: allowed={}, value={}", tenantId, allowed, value);
        return new LeakyBucketResult(allowed, value);
    }

    private String loadScript() {
        try (InputStream is = getClass().getResourceAsStream("/scripts/leaky-bucket.lua")) {
            if (is == null) {
                throw new IllegalStateException("leaky-bucket.lua not found on classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load leaky-bucket.lua", e);
        }
    }
}
