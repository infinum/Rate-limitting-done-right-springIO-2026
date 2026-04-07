package com.infinum.ratelimiting.model;

public record SmsRequest(String phone, String message) {
}
