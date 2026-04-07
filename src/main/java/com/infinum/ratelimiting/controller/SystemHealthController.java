package com.infinum.ratelimiting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.infinum.ratelimiting.service.SystemHealthService;

@RestController
@RequestMapping("/internal")
class SystemHealthController {

    private final SystemHealthService systemHealthService;

    SystemHealthController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    @PostMapping("/health")
    ResponseEntity<Void> setHealth(@RequestParam boolean healthy) {
        systemHealthService.setHealthy(healthy);
        return ResponseEntity.ok().build();
    }
}