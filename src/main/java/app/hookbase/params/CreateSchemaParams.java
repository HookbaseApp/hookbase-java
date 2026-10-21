package app.hookbase.params;

/** Body for {@code schemas().create()}. {@code name} and {@code jsonSchema} are required. */
public final class CreateSchemaParams extends Params<CreateSchemaParams> {
    public CreateSchemaParams name(String name) {
        return set("name", name);
    }

    public CreateSchemaParams slug(String slug) {
        return set("slug", slug);
    }

    public CreateSchemaParams description(String description) {
        return set("description", description);
    }

    /** The JSON Schema document: a {@code Map}, a {@code JsonNode} or any JSON-serializable object. */
    public CreateSchemaParams jsonSchema(Object jsonSchema) {
        return set("jsonSchema", jsonSchema);
    }
}
