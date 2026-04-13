package com.infinum.ratelimiting.service;

public record BurstDecision(boolean allowed, int tokens) {
}
