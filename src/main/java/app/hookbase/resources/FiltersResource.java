package app.hookbase.resources;

import app.hookbase.ApiClient;
import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.models.Filter;
import app.hookbase.models.FilterTestResult;
import app.hookbase.params.CreateFilterParams;
import app.hookbase.params.ListFiltersParams;
import app.hookbase.params.TestFilterParams;
import app.hookbase.params.UpdateFilterParams;
import java.util.LinkedHashMap;
import java.util.Map;

/** Inbound routing filters: conditions a payload must meet for a route to deliver it. */
public final class FiltersResource extends BaseResource {

    private static final String PATH = "/api/filters";

    public FiltersResource(ApiClient http) {
        super(http);
    }

    /** One page of filters (page-numbered, default page 1 of 20). */
    public Page<Filter> list(ListFiltersParams params, RequestOptions options) {
        return page(query(params), options);
    }

    public Page<Filter> list(ListFiltersParams params) {
        return list(params, null);
    }

    public Page<Filter> list() {
        return list(null, null);
    }

    /** Every filter, fetched a page at a time as you iterate, from {@code page} (default 1). */
    public Pager<Filter> listAll(ListFiltersParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        int start = q.remove("page") instanceof Number n ? n.intValue() : 1;
        return Pager.numbered(start, n -> {
            Map<String, Object> page = new LinkedHashMap<>(q);
            page.put("page", n);
            return page(page, options);
        });
    }

    public Pager<Filter> listAll(ListFiltersParams params) {
        return listAll(params, null);
    }

    public Pager<Filter> listAll() {
        return listAll(null, null);
    }

    /** Get a filter by id or slug. */
    public Filter get(String id, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(id), null, options), "filter", Filter.class);
    }

    public Filter get(String id) {
        return get(id, null);
    }

    public Filter create(CreateFilterParams params, RequestOptions options) {
        return unwrap(post(PATH, body(params), options), "filter", Filter.class);
    }

    public Filter create(CreateFilterParams params) {
        return create(params, null);
    }

    /** Update a filter. The API answers with no filter; call {@link #get(String)} to read it back. */
    public void update(String id, UpdateFilterParams params, RequestOptions options) {
        patch(PATH + "/" + seg(id), body(params), options);
    }

    public void update(String id, UpdateFilterParams params) {
        update(id, params, null);
    }

    public void delete(String id, RequestOptions options) {
        delete(PATH + "/" + seg(id), null, options);
    }

    public void delete(String id) {
        delete(id, null);
    }

    /** Evaluate conditions against a sample payload without saving them. */
    public FilterTestResult test(TestFilterParams params, RequestOptions options) {
        return as(post(PATH + "/test", body(params), options), FilterTestResult.class);
    }

    public FilterTestResult test(TestFilterParams params) {
        return test(params, null);
    }

    private Page<Filter> page(Map<String, Object> q, RequestOptions options) {
        int page = q.get("page") instanceof Number n ? n.intValue() : 1;
        int pageSize = q.get("pageSize") instanceof Number n ? n.intValue() : 20;
        return numberedPage(get(PATH, q, options), "filters", Filter.class, page, pageSize);
    }
}
