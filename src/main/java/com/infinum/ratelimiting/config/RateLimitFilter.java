package com.infinum.ratelimiting.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.github.bucket4j.ConsumptionProbe;
import org.springframework.web.filter.OncePerRequestFilter;

import com.infinum.ratelimiting.service.RateLimitResult;
import com.infinum.ratelimiting.service.RateLimitService;

public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenantId = request.getHeader("X-Tenant-Id");
        RateLimitResult result = rateLimitService.tryConsume(tenantId);
        if (result.isConsumed()) {
            ConsumptionProbe probe = result.probe();
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.addHeader("X-Rate-Limit-Source", result.fromBurst() ? "burst" : "primary");
            filterChain.doFilter(request, response);
        } else {
            ConsumptionProbe probe = result.probe();
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            response.setContentType("text/plain");
            response.getWriter().append("Too Many Requests");
        }
    }
}