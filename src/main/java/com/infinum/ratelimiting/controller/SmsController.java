package com.infinum.ratelimiting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infinum.ratelimiting.model.SmsRequest;
import com.infinum.ratelimiting.service.SmsService;

@RestController
@RequestMapping("/api/sms")
class SmsController {

    private final SmsService smsService;

    SmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    @PostMapping("/send")
    ResponseEntity<String> sendOtp(
            @RequestBody SmsRequest request) {
        smsService.send(request.phone(), request.message());
        return ResponseEntity.noContent().build();
    }
}
