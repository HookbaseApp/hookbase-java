package app.hookbase.models;

/**
 * A route's circuit breaker, from {@code routes().getCircuitStatus()}. The API fills defaults for
 * unset settings (cooldown 300 s, failure threshold 5, probe success threshold 2).
 *
 * @param circuitState {@code closed}, {@code open} or {@code half_open}
 * @param probeAttempts probes made since the circuit half-opened
 * @param consecutiveFailures failures in a row so far
 * @param timeUntilProbeSeconds seconds until an open circuit half-opens; {@code null} unless open
 */
public record CircuitStatusInfo(
        String circuitState,
        String circuitOpenedAt,
        int cooldownSeconds,
        int probeAttempts,
        int probeSuccessThreshold,
        int failureThreshold,
        int consecutiveFailures,
        Integer timeUntilProbeSeconds) {}
