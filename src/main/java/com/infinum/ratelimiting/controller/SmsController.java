package com.infinum.ratelimiting.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sms")
class SmsController {

    private static final Logger log = LoggerFactory.getLogger(SmsController.class);

    record SmsRequest(String phone, String message) {}

    @PostMapping("/send")
    ResponseEntity<Void> sendSms(@RequestBody SmsRequest request) {
        log.info("SMS sent to {} with message {}", request.phone(), request.message());
        return ResponseEntity.noContent().build();
    }
}
