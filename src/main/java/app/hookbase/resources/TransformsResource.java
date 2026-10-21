package app.hookbase.resources;

import app.hookbase.ApiClient;
import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.models.Transform;
import app.hookbase.models.TransformTestResult;
import app.hookbase.params.CreateTransformParams;
import app.hookbase.params.ListTransformsParams;
import app.hookbase.params.TestTransformParams;
import app.hookbase.params.UpdateTransformParams;
import java.util.LinkedHashMap;
import java.util.Map;

/** Inbound payload transforms (JSONata, Liquid, XSLT, JavaScript). */
public final class TransformsResource extends BaseResource {

    private static final String PATH = "/api/transforms";

    public TransformsResource(ApiClient http) {
        super(http);
    }

    /** One page of transforms (page-numbered, default page 1 of 20). */
    public Page<Transform> list(ListTransformsParams params, RequestOptions options) {
        return page(query(params), options);
    }

    public Page<Transform> list(ListTransformsParams params) {
        return list(params, null);
    }

    public Page<Transform> list() {
        return list(null, null);
    }

    /** Every transform, fetched a page at a time as you iterate, from {@code page} (default 1). */
    public Pager<Transform> listAll(ListTransformsParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        int start = q.remove("page") instanceof Number n ? n.intValue() : 1;
        return Pager.numbered(start, n -> {
            Map<String, Object> page = new LinkedHashMap<>(q);
            page.put("page", n);
            return page(page, options);
        });
    }

    public Pager<Transform> listAll(ListTransformsParams params) {
        return listAll(params, null);
    }

    public Pager<Transform> listAll() {
        return listAll(null, null);
    }

    /** Get a transform by id or slug. */
    public Transform get(String id, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(id), null, options), "transform", Transform.class);
    }

    public Transform get(String id) {
        return get(id, null);
    }

    public Transform create(CreateTransformParams params, RequestOptions options) {
        return unwrap(post(PATH, body(params), options), "transform", Transform.class);
    }

    public Transform create(CreateTransformParams params) {
        return create(params, null);
    }

    /** Update a transform. The API answers with no transform; call {@link #get(String)} to read it back. */
    public void update(String id, UpdateTransformParams params, RequestOptions options) {
        patch(PATH + "/" + seg(id), body(params), options);
    }

    public void update(String id, UpdateTransformParams params) {
        update(id, params, null);
    }

    public void delete(String id, RequestOptions options) {
        delete(PATH + "/" + seg(id), null, options);
    }

    public void delete(String id) {
        delete(id, null);
    }

    /** Run transform code against a sample payload without saving it. A transform that fails is reported in the result, not thrown. */
    public TransformTestResult test(TestTransformParams params, RequestOptions options) {
        return as(post(PATH + "/test", body(params), options), TransformTestResult.class);
    }

    public TransformTestResult test(TestTransformParams params) {
        return test(params, null);
    }

    private Page<Transform> page(Map<String, Object> q, RequestOptions options) {
        int page = q.get("page") instanceof Number n ? n.intValue() : 1;
        int pageSize = q.get("pageSize") instanceof Number n ? n.intValue() : 20;
        return numberedPage(get(PATH, q, options), "transforms", Transform.class, page, pageSize);
    }
}
