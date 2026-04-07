package com.infinum.ratelimiting.model;

public record SmsRequest(String phoneNumber, String body) {
}
