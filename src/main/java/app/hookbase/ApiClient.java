package app.hookbase;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * What the resource classes send requests through. {@link Hookbase} builds one for you; implement
 * it yourself only to stub the API in tests ({@code new SourcesResource(fakeClient)}).
 */
@FunctionalInterface
public interface ApiClient {

    /**
     * Send one request.
     *
     * @param method the HTTP method, e.g. {@code "GET"}
     * @param path the path from the API root, e.g. {@code "/api/sources"}
     * @param query query parameters; {@code null} values are left out and collections are sent
     *     comma-joined
     * @param body serialized as JSON; {@code null} for none
     * @param options per-request overrides, or {@code null}
     * @return the parsed response, a text node for a non-JSON body (CSV), or {@code null} for 204
     */
    JsonNode request(String method, String path, Map<String, ?> query, Object body, RequestOptions options);
}
