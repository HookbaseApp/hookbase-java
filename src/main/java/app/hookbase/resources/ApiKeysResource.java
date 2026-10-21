package app.hookbase.resources;

import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.ApiKey;
import app.hookbase.models.ApiKeyWithSecret;
import app.hookbase.params.CreateApiKeyParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

/** The organization's API keys. */
public final class ApiKeysResource extends BaseResource {

    private static final String PATH = "/api/api-keys";

    public ApiKeysResource(HttpTransport http) {
        super(http);
    }

    /** Every API key (without secrets). The API returns them all at once, so this is a single page. */
    public Page<ApiKey> list(RequestOptions options) {
        JsonNode node = get(PATH, null, options);
        List<ApiKey> data = list(node, node != null && node.has("apiKeys") ? "apiKeys" : "data", ApiKey.class);
        return new Page<>(data, data.size(), data.size(), 0, false, null);
    }

    public Page<ApiKey> list() {
        return list((RequestOptions) null);
    }

    /** Every API key, as a {@link Pager} (one request). */
    public Pager<ApiKey> listAll(RequestOptions options) {
        return Pager.numbered(1, n -> list(options));
    }

    public Pager<ApiKey> listAll() {
        return listAll(null);
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
