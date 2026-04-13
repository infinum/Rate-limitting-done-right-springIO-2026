package com.infinum.ratelimiting.service;

public record LeakyBucketResult(boolean allowed, long value) {

    /** When allowed, value = remaining capacity. When rejected, value = retry-after in ms. */
}
