package app.hookbase;

import java.time.Duration;

/**
 * Per-request overrides. Every resource method has an overload that takes one.
 *
 * <pre>{@code
 * client.sources().get("src_123", RequestOptions.builder().timeout(Duration.ofSeconds(5)).build());
 * }</pre>
 *
 * @param timeout overrides the client's timeout for this request; {@code null} keeps it
 * @param retries overrides the client's retry count for this request; {@code null} keeps it.
 *     Setting it explicitly also opts a POST in to retries after a 5xx, timeout or dropped
 *     connection, which are otherwise only retried for a POST that sends an event with an
 *     idempotency key
 * @param idempotencyKey sent as the {@code Idempotency-Key} header. The API deduplicates on it only
 *     when sending an event ({@code messages().send()}), so only there does it make a POST safe to
 *     retry; other endpoints ignore it
 */
public record RequestOptions(Duration timeout, Integer retries, String idempotencyKey) {

    /** No overrides. */
    public static final RequestOptions NONE = new RequestOptions(null, null, null);

    public RequestOptions {
        if (retries != null && retries < 0) {
            throw new IllegalArgumentException("retries must be >= 0");
        }
        if (timeout != null && (timeout.isNegative() || timeout.isZero())) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link RequestOptions}. */
    public static final class Builder {
        private Duration timeout;
        private Integer retries;
        private String idempotencyKey;

        private Builder() {}

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder retries(int retries) {
            this.retries = retries;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public RequestOptions build() {
            return new RequestOptions(timeout, retries, idempotencyKey);
        }
    }
}
