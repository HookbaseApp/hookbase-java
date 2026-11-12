package app.hookbase.resources;

import app.hookbase.ApiClient;
import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.internal.SlugFold;
import app.hookbase.models.BulkDeleteResult;
import app.hookbase.models.Destination;
import app.hookbase.models.DestinationSecret;
import app.hookbase.models.DestinationTestResult;
import app.hookbase.models.ImportResult;
import app.hookbase.params.CreateDestinationParams;
import app.hookbase.params.ImportParams;
import app.hookbase.params.ListDestinationsParams;
import app.hookbase.params.RotateDestinationSecretParams;
import app.hookbase.params.UpdateDestinationParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Inbound destinations: where routed events are delivered (HTTP, queues, warehouses, bridges to outbound). */
public final class DestinationsResource extends BaseResource {

    private static final String PATH = "/api/destinations";

    public DestinationsResource(ApiClient http) {
        super(http);
    }

    /** One page of destinations (page 1, 20 per page unless set). */
    public Page<Destination> list(ListDestinationsParams params, RequestOptions options) {
        return listPage(query(params), options);
    }

    public Page<Destination> list(ListDestinationsParams params) {
        return list(params, null);
    }

    public Page<Destination> list() {
        return list(null, null);
    }

    /** Every destination, fetched a page at a time as you iterate, from {@code page} (default 1). */
    public Pager<Destination> listAll(ListDestinationsParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        int start = q.remove("page") instanceof Number n ? n.intValue() : 1;
        return Pager.numbered(start, page -> {
            Map<String, Object> pageQuery = new LinkedHashMap<>(q);
            pageQuery.put("page", page);
            return listPage(pageQuery, options);
        });
    }

    public Pager<Destination> listAll(ListDestinationsParams params) {
        return listAll(params, null);
    }

    public Pager<Destination> listAll() {
        return listAll(null, null);
    }

    private Page<Destination> listPage(Map<String, Object> q, RequestOptions options) {
        int page = q.get("page") instanceof Number n ? n.intValue() : 1;
        int pageSize = q.get("pageSize") instanceof Number n ? n.intValue() : 20;
        return numberedPage(get(PATH, q, options), "destinations", Destination.class, page, pageSize);
    }

    public Destination get(String id, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(id), null, options), "destination", Destination.class);
    }

    public Destination get(String id) {
        return get(id, null);
    }

    /**
     * Create a destination. When {@code slug} is unset or blank, one is derived from {@code name}
     * exactly as the other Hookbase SDKs derive it.
     *
     * <p>An {@code http} destination is created with a signing secret, and this response is the one
     * place it appears in full ({@link Destination#signingSecret()}); later reads carry only
     * {@link Destination#hasSigningSecret()}, and {@link #revealSecret(String)} shows it again.
     *
     * @throws app.hookbase.errors.HookbaseException before any request when no slug is set and the
     *     name has no alphanumeric characters to derive one from
     */
    public Destination create(CreateDestinationParams params, RequestOptions options) {
        return unwrap(post(PATH, createBody(params), options), "destination", Destination.class);
    }

    public Destination create(CreateDestinationParams params) {
        return create(params, null);
    }

    /** Update a destination. The API answers with no destination; {@link #get} it to see the result. */
    public void update(String id, UpdateDestinationParams params, RequestOptions options) {
        patch(PATH + "/" + seg(id), body(params), options);
    }

    public void update(String id, UpdateDestinationParams params) {
        update(id, params, null);
    }

    public void delete(String id, RequestOptions options) {
        delete(PATH + "/" + seg(id), null, options);
    }

    public void delete(String id) {
        delete(id, null);
    }

    /** Export destinations as JSON: the given ids, or every destination when {@code ids} is null or empty. */
    public JsonNode export(List<String> ids, RequestOptions options) {
        return get(PATH + "/export", InboundSupport.exportQuery(ids), options);
    }

    public JsonNode export(List<String> ids) {
        return export(ids, null);
    }

    public JsonNode export() {
        return export(null, null);
    }

    /** Import destinations, in the shape {@link #export} produces (its {@code destinations} array). */
    public ImportResult importDestinations(Iterable<?> destinations, ImportParams params, RequestOptions options) {
        return as(post(PATH + "/import", InboundSupport.importBody("destinations", destinations, params), options),
                ImportResult.class);
    }

    public ImportResult importDestinations(Iterable<?> destinations, ImportParams params) {
        return importDestinations(destinations, params, null);
    }

    public ImportResult importDestinations(Iterable<?> destinations) {
        return importDestinations(destinations, null, null);
    }

    /** Delete up to 100 destinations at once. */
    public BulkDeleteResult bulkDelete(List<String> ids, RequestOptions options) {
        return as(delete(PATH + "/bulk", InboundSupport.ids(ids), options), BulkDeleteResult.class);
    }

    public BulkDeleteResult bulkDelete(List<String> ids) {
        return bulkDelete(ids, null);
    }

    /**
     * Send a test request to the destination (or check a warehouse's configuration). A
     * {@code bridge} destination is dry-run instead, against a sample payload; see
     * {@link #test(String, Object, RequestOptions)}.
     */
    public DestinationTestResult test(String id, RequestOptions options) {
        return test(id, null, options);
    }

    public DestinationTestResult test(String id) {
        return test(id, null, null);
    }

    /**
     * Test the destination with a sample payload. Only a {@code bridge} destination reads it: the
     * API resolves the event type the payload would be published as and counts the endpoints
     * subscribed to it ({@link DestinationTestResult#dryRun()}), without publishing anything. Other
     * destination types send their usual test request and ignore the payload.
     *
     * @param payload a JSON object (a {@code Map} or a Jackson {@code ObjectNode}); {@code null}
     *     sends no body, and a bridge is then dry-run against the API's own test payload
     */
    public DestinationTestResult test(String id, Object payload, RequestOptions options) {
        Map<String, Object> body = null;
        if (payload != null) {
            body = new LinkedHashMap<>();
            body.put("payload", payload);
        }
        return as(post(PATH + "/" + seg(id) + "/test", body, options), DestinationTestResult.class);
    }

    public DestinationTestResult test(String id, Object payload) {
        return test(id, payload, null);
    }

    /**
     * An {@code http} destination's signing secret, in full. Takes a key with the {@code write}
     * scope. A queue, warehouse or bridge destination has none (400), and one created before
     * signing existed has none until {@link #rotateSecret(String)} issues one (404).
     */
    public DestinationSecret revealSecret(String id, RequestOptions options) {
        return as(get(PATH + "/" + seg(id) + "/reveal-secret", null, options), DestinationSecret.class);
    }

    public DestinationSecret revealSecret(String id) {
        return revealSecret(id, null);
    }

    /**
     * Issue a new signing secret for an {@code http} destination. Deliveries are signed with both
     * the new and the previous secret until {@code previousSecretExpiresAt} (one hour unless
     * {@code gracePeriodSeconds} says otherwise), so a receiver can switch without rejecting
     * anything. Takes a key with the {@code write} scope.
     *
     * <p>Not retried after a timeout or 5xx: a rotation that did go through and is then repeated
     * replaces the secret you were just handed.
     */
    public DestinationSecret rotateSecret(String id, RotateDestinationSecretParams params, RequestOptions options) {
        Object body = params == null ? null : body(params);
        return as(post(PATH + "/" + seg(id) + "/rotate-secret", body, options), DestinationSecret.class);
    }

    public DestinationSecret rotateSecret(String id, RotateDestinationSecretParams params) {
        return rotateSecret(id, params, null);
    }

    public DestinationSecret rotateSecret(String id, RequestOptions options) {
        return rotateSecret(id, null, options);
    }

    public DestinationSecret rotateSecret(String id) {
        return rotateSecret(id, (RotateDestinationSecretParams) null, null);
    }

    /** The create body, with {@code slug} derived from {@code name} when it is unset or blank. */
    static Map<String, Object> createBody(CreateDestinationParams params) {
        Map<String, Object> body = body(params);
        Object slug = body.get("slug");
        if (slug == null || (slug instanceof String s && isJsBlank(s))) {
            Object name = body.get("name");
            body.put("slug", SlugFold.deriveDestinationSlug(name == null ? null : name.toString()));
        }
        return body;
    }

    /**
     * {@code s.trim() === ''} exactly as JavaScript means it (Node's check): tab, LF, VT, FF, CR,
     * the space separators (which include NBSP), U+2028/U+2029 and the BOM.
     */
    private static boolean isJsBlank(String s) {
        return s.codePoints().allMatch(cp -> (cp >= 0x09 && cp <= 0x0D) || Character.isSpaceChar(cp) || cp == 0xFEFF);
    }
}
