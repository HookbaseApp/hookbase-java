package app.hookbase.resources;

import app.hookbase.ApiClient;
import app.hookbase.RequestOptions;
import app.hookbase.models.ApiKey;
import app.hookbase.models.ApiKeyWithSecret;
import app.hookbase.params.CreateApiKeyParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

/** The organization's API keys. */
public final class ApiKeysResource extends BaseResource {

    private static final String PATH = "/api/api-keys";

    public ApiKeysResource(ApiClient http) {
        super(http);
    }

    /** Every API key (without secrets). The API returns them all at once. */
    public List<ApiKey> list(RequestOptions options) {
        JsonNode node = get(PATH, null, options);
        return list(node, node != null && node.has("apiKeys") ? "apiKeys" : "data", ApiKey.class);
    }

    public List<ApiKey> list() {
        return list((RequestOptions) null);
    }

    /**
     * Create an API key. The result's {@code key} is the full secret, shown only this once. Needs a
     * signed-in user with the admin or owner role: an API key cannot create keys.
     */
    public ApiKeyWithSecret create(CreateApiKeyParams params, RequestOptions options) {
        JsonNode node = post(PATH, body(params), options);
        if (node == null) return null;
        JsonNode inner = node.has("apiKey") ? node.get("apiKey") : node.has("data") ? node.get("data") : node;
        // Tolerate the secret being sent beside the key object rather than inside it.
        if (inner instanceof ObjectNode obj && !obj.hasNonNull("key") && node.hasNonNull("key")) {
            obj = obj.deepCopy();
            obj.set("key", node.get("key"));
            inner = obj;
        }
        return as(inner, ApiKeyWithSecret.class);
    }

    public ApiKeyWithSecret create(CreateApiKeyParams params) {
        return create(params, null);
    }

    /** Revoke an API key. */
    public void delete(String id, RequestOptions options) {
        delete(PATH + "/" + seg(id), null, options);
    }

    public void delete(String id) {
        delete(id, null);
    }
}
