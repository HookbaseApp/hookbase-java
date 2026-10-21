package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * An inbound route: delivers a source's events to a destination, optionally filtered, transformed
 * and validated.
 *
 * @param filterConditions inline filter conditions ({@code [{"field", "operator", "value"}]}), or
 *     {@code null}; mutually exclusive with {@code filterId}
 * @param filterLogic how inline conditions combine, e.g. {@code and} / {@code or}
 * @param circuitState {@code closed}, {@code open} or {@code half_open}; {@code null} when unset
 * @param notifyEmails comma-separated addresses notified on failure/success/recovery
 * @param failoverDestinationIds destinations tried in order when the primary keeps failing
 * @param failoverAfterAttempts attempts on the primary before failing over
 */
public record Route(
        String id,
        String organizationId,
        String name,
        String sourceId,
        String destinationId,
        String filterId,
        JsonNode filterConditions,
        String filterLogic,
        String transformId,
        String schemaId,
        int priority,
        boolean isActive,
        String circuitState,
        String circuitOpenedAt,
        Integer circuitCooldownSeconds,
        Integer circuitFailureThreshold,
        Integer circuitProbeSuccessThreshold,
        boolean notifyOnFailure,
        boolean notifyOnSuccess,
        boolean notifyOnRecovery,
        String notifyEmails,
        Integer failureThreshold,
        List<String> failoverDestinationIds,
        Integer failoverAfterAttempts,
        String expectedResponse,
        String createdAt,
        String updatedAt) {}
