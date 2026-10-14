package br.com.fiap.delivery.order.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Bonus: limits POST /orders to N requests per second (default 20).
 * Requests over the limit get 429 Too Many Requests with {"error": "..."}.
 */
@Component
public class OrderRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OrderRateLimitFilter.class);
    private static final String BODY = "{\"error\":\"Too many requests, please try again in a moment\"}";

    private final boolean enabled;
    private final TokenBucket bucket;

    public OrderRateLimitFilter(@Value("${app.rate-limit.enabled:true}") boolean enabled,
                                @Value("${app.rate-limit.orders-per-second:20}") long ordersPerSecond) {
        this.enabled = enabled;
        this.bucket = new TokenBucket(ordersPerSecond, ordersPerSecond);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean isCreateOrder = "POST".equalsIgnoreCase(request.getMethod())
                && ("/orders".equals(path) || "/orders/".equals(path));
        return !enabled || !isCreateOrder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (bucket.tryConsume()) {
            chain.doFilter(request, response);
            return;
        }
        log.debug("POST /orders rate limit exceeded");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "1");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(BODY);
    }
}
