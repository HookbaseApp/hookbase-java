package app.hookbase.errors;

import java.time.Duration;
import java.util.Map;

/** HTTP 429. Retried automatically (after {@link #retryAfter()}) until retries run out. */
public final class RateLimitException extends HookbaseApiException {
    private final Duration retryAfter;

    public RateLimitException(String message, Duration retryAfter, String requestId, Map<String, Object> details) {
        super(message, 429, "rate_limit_exceeded", requestId, details);
        this.retryAfter = retryAfter;
    }

    /** How long the API asked the caller to wait ({@code Retry-After}; 60 seconds when absent). */
    public Duration retryAfter() {
        return retryAfter;
    }
}
