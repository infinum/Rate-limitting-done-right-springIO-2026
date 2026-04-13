package com.infinum.ratelimiting.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.infinum.ratelimiting.service.LeakyBucketResult;
import com.infinum.ratelimiting.service.LeakyBucketService;

public class LeakyBucketFilter extends OncePerRequestFilter {

    private final LeakyBucketService leakyBucketService;

    public LeakyBucketFilter(LeakyBucketService leakyBucketService) {
        this.leakyBucketService = leakyBucketService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId == null) {
            tenantId = "anonymous";
        }

        LeakyBucketResult result = leakyBucketService.tryConsume(tenantId);

        if (result.allowed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(result.value()));
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = Math.max(1, result.value() / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("text/plain");
            response.getWriter().append("Too Many Requests");
        }
    }
}
