package app.hookbase.resources;

import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.BulkDeleteResult;
import app.hookbase.models.ImportResult;
import app.hookbase.models.PendingVerification;
import app.hookbase.models.Source;
import app.hookbase.models.SourceWithSecret;
import app.hookbase.params.CreateSourceParams;
import app.hookbase.params.ImportParams;
import app.hookbase.params.ListSourcesParams;
import app.hookbase.params.UpdateSourceParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Inbound sources: the endpoints providers send webhooks to. */
public final class SourcesResource extends BaseResource {

    private static final String PATH = "/api/sources";

    public SourcesResource(HttpTransport http) {
        super(http);
    }

    /** One page of sources (page 1, 20 per page unless set). */
    public Page<Source> list(ListSourcesParams params, RequestOptions options) {
        return listPage(query(params), options);
    }

    public Page<Source> list(ListSourcesParams params) {
        return list(params, null);
    }

    public Page<Source> list() {
        return list(null, null);
    }

    /** Every source, fetched a page at a time as you iterate, from {@code page} (default 1). */
    public Pager<Source> listAll(ListSourcesParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        int start = q.remove("page") instanceof Number n ? n.intValue() : 1;
        return Pager.numbered(start, page -> {
            Map<String, Object> pageQuery = new LinkedHashMap<>(q);
            pageQuery.put("page", page);
            return listPage(pageQuery, options);
        });
    }

    public Pager<Source> listAll(ListSourcesParams params) {
        return listAll(params, null);
    }

    public Pager<Source> listAll() {
        return listAll(null, null);
    }

    private Page<Source> listPage(Map<String, Object> q, RequestOptions options) {
        int page = q.get("page") instanceof Number n ? n.intValue() : 1;
        int pageSize = q.get("pageSize") instanceof Number n ? n.intValue() : 20;
        return numberedPage(get(PATH, q, options), "sources", Source.class, page, pageSize);
    }

    /** A source by id or slug. */
    public Source get(String id, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(id), null, options), "source", Source.class);
    }

    public Source get(String id) {
        return get(id, null);
    }

    /** Create a source. The result carries the signing secret in full, this once. */
    public SourceWithSecret create(CreateSourceParams params, RequestOptions options) {
        return unwrap(post(PATH, body(params), options), "source", SourceWithSecret.class);
    }

    public SourceWithSecret create(CreateSourceParams params) {
        return create(params, null);
    }

    /** Update a source. The API answers with no source; {@link #get} it to see the result. */
    public void update(String id, UpdateSourceParams params, RequestOptions options) {
        patch(PATH + "/" + seg(id), body(params), options);
    }

    public void update(String id, UpdateSourceParams params) {
        update(id, params, null);
    }

    public void delete(String id, RequestOptions options) {
        delete(PATH + "/" + seg(id), null, options);
    }

    public void delete(String id) {
        delete(id, null);
    }

    /**
     * Replace the signing secret with a new Hookbase-generated one, and return it.
     *
     * <p>Only for providers whose secret Hookbase generates (catalog {@code secretSource}
     * {@code operator} or {@code none}). The API answers 400 ({@code ValidationException}) when there
     * is nothing it can generate: providers that verify without a secret ({@code wise},
     * {@code aws-sns}), public-key providers ({@code discord}, {@code sendgrid}...), vendor-issued
     * secrets ({@code stripe}, {@code standard-webhooks}...) and secrets issued after registration
     * ({@code asana}, {@code notion}). Rotate those at the vendor, then {@link #update} the source.
     */
    public String rotateSecret(String id, RequestOptions options) {
        return InboundSupport.text(post(PATH + "/" + seg(id) + "/rotate-secret", null, options), "signingSecret");
    }

    public String rotateSecret(String id) {
        return rotateSecret(id, null);
    }

    /** The signing secret in full. 404 when none is set. */
    public String revealSecret(String id, RequestOptions options) {
        return InboundSupport.text(get(PATH + "/" + seg(id) + "/reveal-secret", null, options), "signingSecret");
    }

    public String revealSecret(String id) {
        return revealSecret(id, null);
    }

    /** The Meta verify token ({@code whatsapp} sources only; 400 for any other provider). */
    public String verifyToken(String id, RequestOptions options) {
        return InboundSupport.text(get(PATH + "/" + seg(id) + "/verify-token", null, options), "verifyToken");
    }

    public String verifyToken(String id) {
        return verifyToken(id, null);
    }

    /** Generate a new Meta verify token, and return it. Update it in Meta's webhook setup afterwards. */
    public String rotateVerifyToken(String id, RequestOptions options) {
        return InboundSupport.text(post(PATH + "/" + seg(id) + "/rotate-verify-token", null, options), "verifyToken");
    }

    public String rotateVerifyToken(String id) {
        return rotateVerifyToken(id, null);
    }

    /**
     * The verification token a vendor (Notion) posted to the ingest URL, awaiting adoption. Throws
     * {@code NotFoundException} when none is waiting (they expire after 24 hours) as well as when
     * the source does not exist.
     */
    public PendingVerification getPendingVerification(String id, RequestOptions options) {
        return as(get(PATH + "/" + seg(id) + "/pending-verification", null, options), PendingVerification.class);
    }

    public PendingVerification getPendingVerification(String id) {
        return getPendingVerification(id, null);
    }

    /**
     * Make the pending verification token this source's signing secret.
     *
     * <p>Do this only after the vendor has accepted the token (Notion's Verify form): it arrived
     * unauthenticated, and that acceptance is what proves it genuine. 404 when none is waiting.
     */
    public void adoptPendingVerification(String id, RequestOptions options) {
        post(PATH + "/" + seg(id) + "/pending-verification/adopt", null, options);
    }

    public void adoptPendingVerification(String id) {
        adoptPendingVerification(id, null);
    }

    /** Export sources as JSON: the given ids, or every source when {@code ids} is null or empty. */
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
     * Import sources, in the shape {@link #export} produces (its {@code sources} array, as a list
     * of maps or the {@code JsonNode} array itself).
     */
    public ImportResult importSources(Iterable<?> sources, ImportParams params, RequestOptions options) {
        return as(post(PATH + "/import", InboundSupport.importBody("sources", sources, params), options), ImportResult.class);
    }

    public ImportResult importSources(Iterable<?> sources, ImportParams params) {
        return importSources(sources, params, null);
    }

    public ImportResult importSources(Iterable<?> sources) {
        return importSources(sources, null, null);
    }

    /** Delete up to 100 sources at once. */
    public BulkDeleteResult bulkDelete(List<String> ids, RequestOptions options) {
        return as(delete(PATH + "/bulk", InboundSupport.ids(ids), options), BulkDeleteResult.class);
    }

    public BulkDeleteResult bulkDelete(List<String> ids) {
        return bulkDelete(ids, null);
    }
}
