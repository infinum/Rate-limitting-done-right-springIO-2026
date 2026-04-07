package com.infinum.ratelimiting.config;

import java.time.Duration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.infinum.ratelimiting.service.RateLimitService;

@Configuration(proxyBeanMethods = false)
class RateLimitConfiguration {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimitService rateLimitService) {
        FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimitFilter(rateLimitService));
        return registrationBean;
    }

    @Bean
    public ProxyManager<String> proxyManager(
            StatefulRedisConnection<String, byte[]> connection) {
        return Bucket4jLettuce.casBasedBuilder(connection)
                .expirationAfterWrite(
                        ExpirationAfterWriteStrategy
                                .basedOnTimeForRefillingBucketUpToMax(
                                        Duration.ofSeconds(60)))
                .build();
    }
}
