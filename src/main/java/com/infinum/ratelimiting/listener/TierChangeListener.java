package com.infinum.ratelimiting.listener;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.infinum.ratelimiting.model.TierChangeMessage;
import com.infinum.ratelimiting.service.RateLimitService;

@Component
public class TierChangeListener {

    private static final Logger log = LoggerFactory.getLogger(TierChangeListener.class);

    private final RateLimitService rateLimitService;

    public TierChangeListener(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @SqsListener("tier-change-queue")
    public void onTierChange(TierChangeMessage message) {
        log.info("Received tier change: tenant='{}', newTier='{}'", message.tenantId(), message.tier());
        rateLimitService.changeTier(message.tenantId(), message.tier());
    }
}
