package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import app.hookbase.internal.Redact;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;

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
        String expiresAt) {
    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("key", Redact.Rule.SECRET));
    }
}
