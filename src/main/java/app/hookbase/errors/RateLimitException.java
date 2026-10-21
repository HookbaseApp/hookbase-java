package app.hookbase.errors;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP 429. Retried automatically after {@link #retryAfter()} when the API sent a delay of at most
 * 60 seconds; thrown at once otherwise (a plan or monthly quota sends no {@code Retry-After}).
 */
public final class RateLimitException extends HookbaseApiException {
    private final Duration retryAfter;

    public RateLimitException(String message, Duration retryAfter, String requestId, Map<String, Object> details) {
        super(message, 429, "rate_limit_exceeded", requestId, details);
        this.retryAfter = retryAfter;
    }

    /** How long the API asked the caller to wait ({@code Retry-After}), or {@code null} if it didn't say. */
    public Duration retryAfter() {
        return retryAfter;
    }
}
