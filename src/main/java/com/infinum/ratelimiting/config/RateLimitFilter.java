package com.infinum.ratelimiting.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.infinum.ratelimiting.service.RateLimitService;

public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {this.rateLimitService = rateLimitService;}

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenantId = request.getHeader("X-Tenant-Id");
        RateLimitService.Result result = rateLimitService.tryConsumeAndReturnRemaining(tenantId);
        if (result.probe().isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(result.probe().getRemainingTokens()));
            response.addHeader("X-Rate-Limit-Source", result.source());
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(result.probe().getNanosToWaitForRefill() / 1_000_000_000));
            response.setHeader("X-Rate-Limit-Source", result.source());
            response.setContentType("text/plain");
            response.getWriter().append("Too Many Requests");
        }
    }
}