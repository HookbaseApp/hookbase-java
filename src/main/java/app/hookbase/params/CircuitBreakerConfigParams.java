package app.hookbase.params;

/** Body for {@code routes().updateCircuitConfig()}. Only the settings you set are changed. */
public final class CircuitBreakerConfigParams extends Params<CircuitBreakerConfigParams> {
    /** How long an open circuit waits before probing, 30-3600 seconds. */
    public CircuitBreakerConfigParams circuitCooldownSeconds(int circuitCooldownSeconds) {
        return set("circuitCooldownSeconds", circuitCooldownSeconds);
    }

    /** Consecutive failures that open the circuit, 1-50. */
    public CircuitBreakerConfigParams circuitFailureThreshold(int circuitFailureThreshold) {
        return set("circuitFailureThreshold", circuitFailureThreshold);
    }

    /** Successful probes that close a half-open circuit, 1-10. */
    public CircuitBreakerConfigParams circuitProbeSuccessThreshold(int circuitProbeSuccessThreshold) {
        return set("circuitProbeSuccessThreshold", circuitProbeSuccessThreshold);
    }
}
