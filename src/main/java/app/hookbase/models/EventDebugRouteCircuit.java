package app.hookbase.models;

/**
 * The circuit breaker of the route an {@link EventDebugDelivery} went through.
 *
 * @param state {@code closed}, {@code open} or {@code half_open}
 * @param openedAt when the circuit last opened; {@code null} when it never has
 * @param cooldownSeconds how long an open circuit waits before probing the destination
 * @param failureThreshold consecutive failures that open the circuit
 * @param consecutiveFailures consecutive failures so far
 */
public record EventDebugRouteCircuit(
        String state, String openedAt, int cooldownSeconds, int failureThreshold, int consecutiveFailures) {}
