package app.hookbase.resources;

import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.BulkDeleteResult;
import app.hookbase.models.CircuitResetResult;
import app.hookbase.models.CircuitStatusInfo;
import app.hookbase.models.ImportResult;
import app.hookbase.models.Route;
import app.hookbase.models.RouteBulkUpdateResult;
import app.hookbase.params.CircuitBreakerConfigParams;
import app.hookbase.params.CreateRouteParams;
import app.hookbase.params.ImportParams;
import app.hookbase.params.ListRoutesParams;
import app.hookbase.params.UpdateRouteParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Inbound routes: which source's events go to which destination, filtered and transformed how. */
public final class RoutesResource extends BaseResource {

    private static final String PATH = "/api/routes";

    public RoutesResource(HttpTransport http) {
        super(http);
    }

    /** One page of routes (page 1, 20 per page unless set). */
    public Page<Route> list(ListRoutesParams params, RequestOptions options) {
        return listPage(query(params), options);
    }

    public Page<Route> list(ListRoutesParams params) {
        return list(params, null);
    }

    public Page<Route> list() {
        return list(null, null);
    }

    /** Every route, fetched a page at a time as you iterate, from {@code page} (default 1). */
    public Pager<Route> listAll(ListRoutesParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        int start = q.remove("page") instanceof Number n ? n.intValue() : 1;
        return Pager.numbered(start, page -> {
            Map<String, Object> pageQuery = new LinkedHashMap<>(q);
            pageQuery.put("page", page);
            return listPage(pageQuery, options);
        });
    }

    public Pager<Route> listAll(ListRoutesParams params) {
        return listAll(params, null);
    }

    public Pager<Route> listAll() {
        return listAll(null, null);
    }

    private Page<Route> listPage(Map<String, Object> q, RequestOptions options) {
        int page = q.get("page") instanceof Number n ? n.intValue() : 1;
        int pageSize = q.get("pageSize") instanceof Number n ? n.intValue() : 20;
        return numberedPage(get(PATH, q, options), "routes", Route.class, page, pageSize);
    }

    public Route get(String id, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(id), null, options), "route", Route.class);
    }

    public Route get(String id) {
        return get(id, null);
    }

    public Route create(CreateRouteParams params, RequestOptions options) {
        return unwrap(post(PATH, body(params), options), "route", Route.class);
    }

    public Route create(CreateRouteParams params) {
        return create(params, null);
    }

    /** Update a route. The API answers with no route; {@link #get} it to see the result. */
    public void update(String id, UpdateRouteParams params, RequestOptions options) {
        patch(PATH + "/" + seg(id), body(params), options);
    }

    public void update(String id, UpdateRouteParams params) {
        update(id, params, null);
    }

    public void delete(String id, RequestOptions options) {
        delete(PATH + "/" + seg(id), null, options);
    }

    public void delete(String id) {
        delete(id, null);
    }

    /** Delete up to 100 routes at once. */
    public BulkDeleteResult bulkDelete(List<String> ids, RequestOptions options) {
        return as(delete(PATH + "/bulk", InboundSupport.ids(ids), options), BulkDeleteResult.class);
    }

    public BulkDeleteResult bulkDelete(List<String> ids) {
        return bulkDelete(ids, null);
    }

    /** Enable or disable up to 100 routes at once. */
    public RouteBulkUpdateResult bulkUpdate(List<String> ids, boolean isActive, RequestOptions options) {
        Map<String, Object> body = InboundSupport.ids(ids);
        body.put("isActive", isActive);
        return as(patch(PATH + "/bulk", body, options), RouteBulkUpdateResult.class);
    }

    public RouteBulkUpdateResult bulkUpdate(List<String> ids, boolean isActive) {
        return bulkUpdate(ids, isActive, null);
    }

    /** Export routes as JSON: the given ids, or every route when {@code ids} is null or empty. */
    public JsonNode export(List<String> ids, RequestOptions options) {
        return get(PATH + "/export", InboundSupport.exportQuery(ids), options);
    }

    public JsonNode export(List<String> ids) {
        return export(ids, null);
    }

    public JsonNode export() {
        return export(null, null);
    }

    /**
     * Import routes, in the shape {@link #export} produces (its {@code routes} array: sources,
     * destinations, filters, transforms and schemas are referenced by slug and must exist).
     */
    public ImportResult importRoutes(Iterable<?> routes, ImportParams params, RequestOptions options) {
        return as(post(PATH + "/import", InboundSupport.importBody("routes", routes, params), options), ImportResult.class);
    }

    public ImportResult importRoutes(Iterable<?> routes, ImportParams params) {
        return importRoutes(routes, params, null);
    }

    public ImportResult importRoutes(Iterable<?> routes) {
        return importRoutes(routes, null, null);
    }

    /** The route's circuit breaker state and settings. */
    public CircuitStatusInfo getCircuitStatus(String routeId, RequestOptions options) {
        return as(get(PATH + "/" + seg(routeId) + "/circuit-status", null, options), CircuitStatusInfo.class);
    }

    public CircuitStatusInfo getCircuitStatus(String routeId) {
        return getCircuitStatus(routeId, null);
    }

    /** Reset (close) the route's circuit breaker. */
    public CircuitResetResult resetCircuit(String routeId, RequestOptions options) {
        return as(post(PATH + "/" + seg(routeId) + "/reset-circuit", null, options), CircuitResetResult.class);
    }

    public CircuitResetResult resetCircuit(String routeId) {
        return resetCircuit(routeId, null);
    }

    /** Change the route's circuit breaker settings. */
    public void updateCircuitConfig(String routeId, CircuitBreakerConfigParams config, RequestOptions options) {
        patch(PATH + "/" + seg(routeId) + "/circuit-config", body(config), options);
    }

    public void updateCircuitConfig(String routeId, CircuitBreakerConfigParams config) {
        updateCircuitConfig(routeId, config, null);
    }
}
