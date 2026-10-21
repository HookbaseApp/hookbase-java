package app.hookbase.params;

import java.util.List;

/** Body for {@code apiKeys().create()}. {@code name} is required. */
public final class CreateApiKeyParams extends Params<CreateApiKeyParams> {
    public CreateApiKeyParams name(String name) {
        return set("name", name);
    }

    /** {@code read}, {@code write}, {@code delete} (default {@code read} and {@code write}). */
    public CreateApiKeyParams scopes(List<String> scopes) {
        return set("scopes", scopes);
    }

    /**
     * Days until the key expires (default: never). Sent as the API's {@code expiresIn}, which is in
     * seconds; the API ignores any other expiry key, so a key asked for with one never expires.
     */
    public CreateApiKeyParams expiresInDays(int days) {
        return set("expiresIn", days * 24L * 60 * 60);
    }
}
