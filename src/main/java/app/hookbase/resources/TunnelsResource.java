package app.hookbase.resources;

import app.hookbase.ApiClient;
import app.hookbase.RequestOptions;
import app.hookbase.models.Tunnel;
import app.hookbase.models.TunnelBulkDeleteResult;
import app.hookbase.params.CreateTunnelParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/** Local-development tunnels (connected with the CLI). */
public final class TunnelsResource extends BaseResource {

    private static final String PATH = "/api/tunnels";

    public TunnelsResource(ApiClient http) {
        super(http);
    }

    /** Every tunnel. The API returns them all at once. */
    public List<Tunnel> list(RequestOptions options) {
        JsonNode node = get(PATH, null, options);
        return list(node, node != null && node.has("tunnels") ? "tunnels" : "data", Tunnel.class);
    }

    public List<Tunnel> list() {
        return list((RequestOptions) null);
    }

    public Tunnel get(String id, RequestOptions options) {
        return tunnel(get(PATH + "/" + seg(id), null, options));
    }

    public Tunnel get(String id) {
        return get(id, null);
    }

    /** Create a tunnel. The result's {@code authToken} is what the CLI connects with, shown only this once. */
    public Tunnel create(CreateTunnelParams params, RequestOptions options) {
        return tunnel(post(PATH, body(params), options));
    }

    public Tunnel create(CreateTunnelParams params) {
        return create(params, null);
    }

    /** Delete up to 100 tunnels. All must exist in the organization, or none is deleted. */
    public TunnelBulkDeleteResult bulkDelete(List<String> ids, RequestOptions options) {
        return as(delete(PATH + "/bulk", Map.of("ids", ids), options), TunnelBulkDeleteResult.class);
    }

    public TunnelBulkDeleteResult bulkDelete(List<String> ids) {
        return bulkDelete(ids, null);
    }

    private static Tunnel tunnel(JsonNode node) {
        return unwrap(node, node != null && node.has("tunnel") ? "tunnel" : "data", Tunnel.class);
    }
}
