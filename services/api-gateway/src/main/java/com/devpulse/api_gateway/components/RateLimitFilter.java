package com.devpulse.api_gateway.components;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final BucketConfiguration bucketConfiguration;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("========== GATEWAY RATE LIMIT FILTER ========== path: {}", request.getRequestURI());

        String path = request.getRequestURI();

        if(path.startsWith("/user/") || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteUser();

        //in case of load-testing this can be used
//        String userId = request.getHeader("X-User-Id");
//        String bucketKey = (userId != null) ? "user:" + userId : "ip:" + request.getRemoteAddr();

        Bucket bucket = proxyManager.getProxy(ip, () -> bucketConfiguration);

        try{
            if(bucket.tryConsume(1))
                filterChain.doFilter(request, response);
            else {
                log.warn("Rate limit exceeded for IP: {}", ip);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.addHeader("Retry-After", "60");
                response.getWriter().write("{\"error\": \"Too many requests\"}");
            }
        } catch (Exception e) {
            log.error("Rate limiter error: {}", e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("{\"error\": \"Rate limiter unavailable\"}");
        }


    }
}
