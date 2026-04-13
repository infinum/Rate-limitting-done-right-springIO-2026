package com.infinum.ratelimiting.config;

import java.time.Duration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class RedisConfiguration {

    @Bean
    public RedisClient redisClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return RedisClient.create(RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withTimeout(Duration.ofSeconds(1))
                .build());
    }

    @Bean
    public StatefulRedisConnection<String, String> redisConnection(RedisClient redisClient) {
        return redisClient.connect();
    }
}
