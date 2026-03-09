package com.example.todo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a unique traceId to every incoming request via MDC.
 * All log lines within the same request will share the same traceId,
 * making it easy to correlate logs for a single request.
 */
@Component
@Order(1)
@Slf4j
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);

        try {
            // Put traceId into MDC so all log lines in this request include it
            MDC.put(TRACE_ID_KEY, traceId);

            // Return the traceId in the response header so clients can reference it
            response.setHeader(TRACE_ID_HEADER, traceId);

            log.info("Incoming request: method={} uri={} traceId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    traceId);

            filterChain.doFilter(request, response);

            log.info("Completed request: method={} uri={} status={} traceId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    traceId);

        } finally {
            // CRITICAL - always clear MDC after request to prevent leaking
            // traceId into other requests on the same thread (thread pool reuse)
            MDC.clear();
        }
    }

    /**
     * Uses X-Trace-Id from the incoming request header if provided (e.g. from API gateway),
     * otherwise generates a new UUID.
     */
    private String resolveTraceId(HttpServletRequest request) {
        String existingTraceId = request.getHeader(TRACE_ID_HEADER);
        return (existingTraceId != null && !existingTraceId.isBlank())
                ? existingTraceId
                : UUID.randomUUID().toString();
    }
}