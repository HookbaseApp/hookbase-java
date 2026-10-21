package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A JSON schema that routes validate payloads against.
 *
 * @param jsonSchema the JSON Schema document
 */
public record Schema(
        String id,
        String organizationId,
        String name,
        String slug,
        String description,
        @JsonAlias("json_schema") @JsonDeserialize(using = JsonTextDeserializer.class) JsonNode jsonSchema,
        String createdAt,
        String updatedAt) {}
