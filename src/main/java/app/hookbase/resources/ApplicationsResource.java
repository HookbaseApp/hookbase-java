package app.hookbase.resources;

import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.Application;
import app.hookbase.params.CreateApplicationParams;
import app.hookbase.params.ListApplicationsParams;
import app.hookbase.params.UpdateApplicationParams;
import java.util.LinkedHashMap;
import java.util.Map;

/** Outbound applications: your customers or tenants, which group their endpoints. */
public final class ApplicationsResource extends BaseResource {

    private static final String PATH = "/api/webhook-applications";

    public ApplicationsResource(HttpTransport http) {
        super(http);
    }

    /** One page of applications. Pass the returned {@link Page#cursor()} back as {@code cursor} for the next. */
    public Page<Application> list(ListApplicationsParams params, RequestOptions options) {
        return cursorPage(PATH, query(params), options, Application.class, null);
    }

    public Page<Application> list(ListApplicationsParams params) {
        return list(params, null);
    }

    public Page<Application> list() {
        return list(null, null);
    }

    /** Every application, fetched 100 at a time (or {@code limit}) as you iterate. */
    public Pager<Application> listAll(ListApplicationsParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        q.putIfAbsent("limit", 100);
        String start = (String) q.remove("cursor");
        return Pager.cursor(start, cursor -> {
            Map<String, Object> page = new LinkedHashMap<>(q);
            page.put("cursor", cursor);
            return cursorPage(PATH, page, options, Application.class, null);
        });
    }

    public Pager<Application> listAll(ListApplicationsParams params) {
        return listAll(params, null);
    }

    public Pager<Application> listAll() {
        return listAll(null, null);
    }

    public Application get(String id, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(id), null, options), "data", Application.class);
    }

    public Application get(String id) {
        return get(id, null);
    }

    /** Look an application up by the {@code externalId} you gave it. */
    public Application getByUid(String externalId, RequestOptions options) {
        return unwrap(get(PATH + "/by-external-id/" + seg(externalId), null, options), "data", Application.class);
    }

    public Application getByUid(String externalId) {
        return getByUid(externalId, null);
    }

    public Application create(CreateApplicationParams params, RequestOptions options) {
        return unwrap(post(PATH, body(params), options), "data", Application.class);
    }

    public Application create(CreateApplicationParams params) {
        return create(params, null);
    }

    public Application update(String id, UpdateApplicationParams params, RequestOptions options) {
        return unwrap(patch(PATH + "/" + seg(id), body(params), options), "data", Application.class);
    }

    public Application update(String id, UpdateApplicationParams params) {
        return update(id, params, null);
    }

    /** Delete an application with all its endpoints and subscriptions. */
    public void delete(String id, RequestOptions options) {
        delete(PATH + "/" + seg(id), null, options);
    }

    public void delete(String id) {
        delete(id, null);
    }

    /**
     * The application with this {@code externalId}, created from {@code params} if there is none
     * (the API's upsert, {@code PUT /api/webhook-applications/upsert}). Safe to call on every
     * request; {@code params}' own externalId, if set, is overridden.
     */
    public Application getOrCreate(String externalId, CreateApplicationParams params, RequestOptions options) {
        LinkedHashMap<String, Object> body = body(params);
        body.put("externalId", externalId);
        return unwrap(put(PATH + "/upsert", body, options), "data", Application.class);
    }

    public Application getOrCreate(String externalId, CreateApplicationParams params) {
        return getOrCreate(externalId, params, null);
    }
}
