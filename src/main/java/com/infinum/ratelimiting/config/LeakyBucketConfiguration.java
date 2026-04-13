package com.infinum.ratelimiting.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.infinum.ratelimiting.service.LeakyBucketService;

@Configuration(proxyBeanMethods = false)
class LeakyBucketConfiguration {

    @Bean
    public FilterRegistrationBean<LeakyBucketFilter> leakyBucketFilter(LeakyBucketService leakyBucketService) {
        FilterRegistrationBean<LeakyBucketFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LeakyBucketFilter(leakyBucketService));
        registrationBean.addUrlPatterns("/api/*");
        return registrationBean;
    }
}
