package app.hookbase.params;

/** Body for {@code schemas().update()}. Only the fields you set are changed; set {@code description} to {@code null} to clear it. */
public final class UpdateSchemaParams extends Params<UpdateSchemaParams> {
    public UpdateSchemaParams name(String name) {
        return set("name", name);
    }

    public UpdateSchemaParams description(String description) {
        return set("description", description);
    }

    /** Replaces the JSON Schema document. */
    public UpdateSchemaParams jsonSchema(Object jsonSchema) {
        return set("jsonSchema", jsonSchema);
    }
}
