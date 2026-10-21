package app.hookbase.models;

/**
 * Result of {@code routes().resetCircuit()}.
 *
 * @param circuitState always {@code closed} after a reset
 * @param previousState the state before the reset ({@code null} if it had never tripped)
 */
public record CircuitResetResult(boolean success, String circuitState, String previousState) {}
