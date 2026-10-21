package app.hookbase.resources;

import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.Tunnel;
import app.hookbase.models.TunnelBulkDeleteResult;
import app.hookbase.params.CreateTunnelParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/** Local-development tunnels (connected with the CLI). */
public final class TunnelsResource extends BaseResource {

    private static final String PATH = "/api/tunnels";

    public TunnelsResource(HttpTransport http) {
        super(http);
    }

    /** Every tunnel. The API returns them all at once, so this is a single page. */
    public Page<Tunnel> list(RequestOptions options) {
        JsonNode node = get(PATH, null, options);
        List<Tunnel> data = list(node, node != null && node.has("tunnels") ? "tunnels" : "data", Tunnel.class);
        return new Page<>(data, data.size(), data.size(), 0, false, null);
    }

    public Page<Tunnel> list() {
        return list((RequestOptions) null);
    }

    /** Every tunnel, as a {@link Pager} (one request). */
    public Pager<Tunnel> listAll(RequestOptions options) {
        return Pager.numbered(1, n -> list(options));
    }

    public Pager<Tunnel> listAll() {
        return listAll(null);
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
