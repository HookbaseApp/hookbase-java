package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/**
 * An inbound destination: where routed events are delivered.
 *
 * @param type {@code http}, a queue ({@code sqs}, {@code eventbridge}, {@code servicebus},
 *     {@code pubsub}, {@code oci_queue}) or a warehouse ({@code s3}, {@code r2}, {@code gcs},
 *     {@code azure_blob})
 * @param url empty for queue and warehouse destinations
 * @param method {@code GET}, {@code POST}, {@code PUT}, {@code PATCH} or {@code DELETE}
 * @param headers sent as a name-to-value record
 * @param authType {@code none}, {@code basic}, {@code bearer}, {@code api_key} or {@code custom_header}
 * @param timeoutMs request timeout in milliseconds
 * @param config the queue or warehouse configuration (bucket, region, credentials, prefix, ...);
 *     {@code null} for HTTP destinations
 * @param fieldMapping warehouse column mapping, or {@code null}
 * @param routeCount routes pointing at this destination
 */
public record Destination(
        String id,
        String organizationId,
        String name,
        String slug,
        String type,
        String url,
        String method,
        Map<String, String> headers,
        String authType,
        JsonNode authConfig,
        int timeoutMs,
        DestinationThrottle throttle,
        boolean isActive,
        boolean useStaticIp,
        JsonNode config,
        List<DestinationFieldMapping> fieldMapping,
        Integer batchSize,
        Integer batchWindowSeconds,
        long deliveryCount,
        Integer routeCount,
        Long successCount,
        Long failureCount,
        String createdAt,
        String updatedAt) {}
