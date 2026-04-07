package com.infinum.ratelimiting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    public void send(String phone, String message) {
        log.info("SMS sent to {} with message {}", phone, message);
    }
}
