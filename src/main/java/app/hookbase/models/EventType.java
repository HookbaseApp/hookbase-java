package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * An outbound event type, such as {@code order.created}, that endpoints subscribe to.
 *
 * @param schema JSON Schema for the payload, or {@code null}; decoded when the API sends it as JSON text
 * @param examplePayload example payload, or {@code null}; decoded when the API sends it as JSON text
 * @param defaultPriority 0=Critical, 1=High, 2=Normal, 3=Low, 4=Bulk; {@code null} for the platform default
 * @param isEnabled {@code archive()} sets this to false, {@code unarchive()} back to true
 * @param subscriptionCount set on list responses only
 */
public record EventType(
        String id,
        String organizationId,
        String name,
        String displayName,
        String description,
        String category,
        @JsonDeserialize(using = JsonTextDeserializer.class) JsonNode schema,
        Integer schemaVersion,
        @JsonDeserialize(using = JsonTextDeserializer.class) JsonNode examplePayload,
        String documentationUrl,
        Integer defaultPriority,
        boolean isEnabled,
        boolean isDeprecated,
        String deprecatedAt,
        String deprecatedMessage,
        boolean isPublic,
        String publishedAt,
        Integer subscriptionCount,
        String createdAt,
        String updatedAt) {}
