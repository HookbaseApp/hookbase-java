package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

/**
 * A newly created API key. {@code key} is the full secret: it is returned here once and cannot be
 * read again.
 *
 * @param expiresAt {@code null} for a key that never expires
 */
public record ApiKeyWithSecret(
        String id,
        String name,
        String key,
        String keyPrefix,
        @JsonDeserialize(using = JsonTextDeserializer.class) List<String> scopes,
        String expiresAt) {}
