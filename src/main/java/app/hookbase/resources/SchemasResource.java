package app.hookbase.resources;

import app.hookbase.ApiClient;
import app.hookbase.RequestOptions;
import app.hookbase.models.Schema;
import app.hookbase.models.SchemaValidationResult;
import app.hookbase.params.CreateSchemaParams;
import app.hookbase.params.UpdateSchemaParams;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON schemas that routes validate inbound payloads against. */
public final class SchemasResource extends BaseResource {

    private static final String PATH = "/api/schemas";

    public SchemasResource(ApiClient http) {
        super(http);
    }

    /** Every schema. The API returns them all at once. */
    public List<Schema> list(RequestOptions options) {
        return list(get(PATH, null, options), "schemas", Schema.class);
    }

    public List<Schema> list() {
        return list((RequestOptions) null);
    }

    /** Get a schema by id or slug. */
    public Schema get(String id, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(id), null, options), "schema", Schema.class);
    }

    public Schema get(String id) {
        return get(id, null);
    }

    public Schema create(CreateSchemaParams params, RequestOptions options) {
        return unwrap(post(PATH, body(params), options), "schema", Schema.class);
    }

    public Schema create(CreateSchemaParams params) {
        return create(params, null);
    }

    /** Update a schema ({@code PUT}, but only the fields you set change). Renaming also changes the slug. */
    public void update(String id, UpdateSchemaParams params, RequestOptions options) {
        put(PATH + "/" + seg(id), body(params), options);
    }

    public void update(String id, UpdateSchemaParams params) {
        update(id, params, null);
    }

    public void delete(String id, RequestOptions options) {
        delete(PATH + "/" + seg(id), null, options);
    }

    public void delete(String id) {
        delete(id, null);
    }

    /** Validate a payload (any JSON-serializable value) against a schema. */
    public SchemaValidationResult validate(String id, Object payload, RequestOptions options) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payload", payload);
        return as(post(PATH + "/" + seg(id) + "/validate", body, options), SchemaValidationResult.class);
    }

    public SchemaValidationResult validate(String id, Object payload) {
        return validate(id, payload, null);
    }
}
