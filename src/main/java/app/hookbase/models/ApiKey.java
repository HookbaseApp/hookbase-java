package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

/**
 * An API key, without its secret.
 *
 * @param keyPrefix the first characters of the key ({@code whr_…}), to tell keys apart
 * @param scopes {@code read}, {@code write}, {@code delete}
 * @param expiresAt {@code null} for a key that never expires
 */
public record ApiKey(
        String id,
        String organizationId,
        String name,
        String keyPrefix,
        @JsonDeserialize(using = JsonTextDeserializer.class) List<String> scopes,
        String lastUsedAt,
        String expiresAt,
        String createdBy,
        String createdAt) {}
