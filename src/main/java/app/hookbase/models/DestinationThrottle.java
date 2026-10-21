package app.hookbase.models;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A destination's delivery throttle, as returned and as sent with {@code throttle(...)} on the
 * create and update params. Unset components are left out of a request.
 *
 * @param mode {@code off}, {@code rate} or {@code concurrency}
 * @param rateLimit deliveries per {@code rateUnit}; required for {@code rate}
 * @param rateUnit {@code second}, {@code minute} or {@code hour}; required for {@code rate}
 * @param maxConcurrency deliveries in flight at once; required for {@code concurrency}
 * @param queueLimit how many deliveries may wait before new ones are refused
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DestinationThrottle(String mode, Integer rateLimit, String rateUnit, Integer maxConcurrency, Integer queueLimit) {

    /** No throttling. */
    public static DestinationThrottle off() {
        return new DestinationThrottle("off", null, null, null, null);
    }

    /** At most {@code rateLimit} deliveries per {@code rateUnit} ({@code second}, {@code minute}, {@code hour}). */
    public static DestinationThrottle rate(int rateLimit, String rateUnit) {
        return new DestinationThrottle("rate", rateLimit, rateUnit, null, null);
    }

    /** At most {@code maxConcurrency} deliveries in flight at once. */
    public static DestinationThrottle concurrency(int maxConcurrency) {
        return new DestinationThrottle("concurrency", null, null, maxConcurrency, null);
    }

    /** This throttle with a queue limit. */
    public DestinationThrottle withQueueLimit(int queueLimit) {
        return new DestinationThrottle(mode, rateLimit, rateUnit, maxConcurrency, queueLimit);
    }
}
