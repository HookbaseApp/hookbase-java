package app.hookbase.resources;

import app.hookbase.ApiClient;
import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.models.Endpoint;
import app.hookbase.models.EndpointRotateSecretResult;
import app.hookbase.models.EndpointStats;
import app.hookbase.params.CreateEndpointParams;
import app.hookbase.params.ListEndpointsParams;
import app.hookbase.params.RotateEndpointSecretParams;
import app.hookbase.params.TestEndpointParams;
import app.hookbase.params.UpdateEndpointParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outbound webhook endpoints: the URLs your customers receive events on.
 *
 * <p>Every method takes the endpoint's {@code applicationId} first, as in the other Hookbase SDKs.
 * Only {@code list}, {@code listAll} and {@code create} send it; the API finds an endpoint by its id
 * alone.
 */
public final class EndpointsResource extends BaseResource {

    private static final String PATH = "/api/webhook-endpoints";

    public EndpointsResource(ApiClient http) {
        super(http);
    }

    /** One page of an application's endpoints. Pass the returned {@link Page#cursor()} back as {@code cursor} for the next. */
    public Page<Endpoint> list(String applicationId, ListEndpointsParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        q.put("applicationId", applicationId);
        return cursorPage(PATH, q, options, Endpoint.class, null);
    }

    public Page<Endpoint> list(String applicationId, ListEndpointsParams params) {
        return list(applicationId, params, null);
    }

    public Page<Endpoint> list(String applicationId) {
        return list(applicationId, null, null);
    }

    /** Every endpoint of an application, fetched 100 at a time (or {@code limit}) as you iterate. */
    public Pager<Endpoint> listAll(String applicationId, ListEndpointsParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        q.put("applicationId", applicationId);
        q.putIfAbsent("limit", 100);
        String start = (String) q.remove("cursor");
        return Pager.cursor(start, cursor -> {
            Map<String, Object> page = new LinkedHashMap<>(q);
            page.put("cursor", cursor);
            return cursorPage(PATH, page, options, Endpoint.class, null);
        });
    }

    public Pager<Endpoint> listAll(String applicationId, ListEndpointsParams params) {
        return listAll(applicationId, params, null);
    }

    public Pager<Endpoint> listAll(String applicationId) {
        return listAll(applicationId, null, null);
    }

    public Endpoint get(String applicationId, String endpointId, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(endpointId), (Map<String, ?>) null, options), "data", Endpoint.class);
    }

    public Endpoint get(String applicationId, String endpointId) {
        return get(applicationId, endpointId, null);
    }

    /**
     * Create an endpoint. The response is the one place the plaintext signing secret appears
     * ({@link Endpoint#secret()}); every later read shows only {@code secretPrefix}.
     */
    public Endpoint create(String applicationId, CreateEndpointParams params, RequestOptions options) {
        LinkedHashMap<String, Object> body = body(params);
        body.put("applicationId", applicationId);
        return unwrap(post(PATH, body, options), "data", Endpoint.class);
    }

    public Endpoint create(String applicationId, CreateEndpointParams params) {
        return create(applicationId, params, null);
    }

    public Endpoint update(String applicationId, String endpointId, UpdateEndpointParams params, RequestOptions options) {
        return unwrap(patch(PATH + "/" + seg(endpointId), body(params), options), "data", Endpoint.class);
    }

    public Endpoint update(String applicationId, String endpointId, UpdateEndpointParams params) {
        return update(applicationId, endpointId, params, null);
    }

    public void delete(String applicationId, String endpointId, RequestOptions options) {
        delete(PATH + "/" + seg(endpointId), (Object) null, options);
    }

    public void delete(String applicationId, String endpointId) {
        delete(applicationId, endpointId, null);
    }

    /**
     * Rotate the endpoint's signing secret. The API keeps signing with the previous secret as well
     * until {@code previousSecretExpiresAt} (one hour unless {@code gracePeriodSeconds} says
     * otherwise).
     *
     * <p>Not retried after a timeout or 5xx: a rotation that did go through and is then repeated
     * replaces the secret you were just handed.
     */
    public EndpointRotateSecretResult rotateSecret(
            String applicationId, String endpointId, RotateEndpointSecretParams params, RequestOptions options) {
        Object body = params == null ? null : body(params);
        return as(post(PATH + "/" + seg(endpointId) + "/rotate-secret", body, options), EndpointRotateSecretResult.class);
    }

    public EndpointRotateSecretResult rotateSecret(String applicationId, String endpointId, RotateEndpointSecretParams params) {
        return rotateSecret(applicationId, endpointId, params, null);
    }

    public EndpointRotateSecretResult rotateSecret(String applicationId, String endpointId, RequestOptions options) {
        return rotateSecret(applicationId, endpointId, null, options);
    }

    public EndpointRotateSecretResult rotateSecret(String applicationId, String endpointId) {
        return rotateSecret(applicationId, endpointId, (RotateEndpointSecretParams) null, null);
    }

    /** Enable the endpoint (an update that sends only {@code isDisabled: false}). */
    public Endpoint enable(String applicationId, String endpointId, RequestOptions options) {
        return update(applicationId, endpointId, new UpdateEndpointParams().isDisabled(false), options);
    }

    public Endpoint enable(String applicationId, String endpointId) {
        return enable(applicationId, endpointId, null);
    }

    /** Disable the endpoint (an update that sends only {@code isDisabled: true}). */
    public Endpoint disable(String applicationId, String endpointId, RequestOptions options) {
        return update(applicationId, endpointId, new UpdateEndpointParams().isDisabled(true), options);
    }

    public Endpoint disable(String applicationId, String endpointId) {
        return disable(applicationId, endpointId, null);
    }

    /** Delivery statistics, computed from the endpoint's counters (one {@code get}). */
    public EndpointStats getStats(String applicationId, String endpointId, RequestOptions options) {
        Endpoint ep = get(applicationId, endpointId, options);
        double successRate = ep.totalMessages() > 0 ? (ep.totalSuccesses() / (double) ep.totalMessages()) * 100 : 0;
        double averageLatency = ep.avgResponseTimeMs() != null ? ep.avgResponseTimeMs() : 0;
        return new EndpointStats(ep.totalMessages(), ep.totalSuccesses(), ep.totalFailures(), successRate, averageLatency);
    }

    public EndpointStats getStats(String applicationId, String endpointId) {
        return getStats(applicationId, endpointId, null);
    }

    /** Reset the endpoint's circuit breaker to closed, then return the endpoint as it now stands. */
    public Endpoint recoverCircuit(String applicationId, String endpointId, RequestOptions options) {
        post(PATH + "/" + seg(endpointId) + "/reset-circuit", null, options);
        return get(applicationId, endpointId, options);
    }

    public Endpoint recoverCircuit(String applicationId, String endpointId) {
        return recoverCircuit(applicationId, endpointId, null);
    }

    /**
     * Send a test event to the endpoint and return the API's report of the delivery. With no
     * {@code params} (or none set) no body is sent, and the API sends a sample event.
     */
    public JsonNode test(String applicationId, String endpointId, TestEndpointParams params, RequestOptions options) {
        Object body = params == null || params.toMap().isEmpty() ? null : body(params);
        return post(PATH + "/" + seg(endpointId) + "/test", body, options);
    }

    public JsonNode test(String applicationId, String endpointId, TestEndpointParams params) {
        return test(applicationId, endpointId, params, null);
    }

    public JsonNode test(String applicationId, String endpointId, RequestOptions options) {
        return test(applicationId, endpointId, null, options);
    }

    public JsonNode test(String applicationId, String endpointId) {
        return test(applicationId, endpointId, (TestEndpointParams) null, null);
    }
}
