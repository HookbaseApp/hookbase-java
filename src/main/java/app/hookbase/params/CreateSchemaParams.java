package app.hookbase.params;

/** Body for {@code schemas().create()}. {@code name} and {@code jsonSchema} are required. */
public final class CreateSchemaParams extends Params<CreateSchemaParams> {
    public CreateSchemaParams name(String name) {
        return set("name", name);
    }

    /**
     * @deprecated The API ignores this: it derives the slug from {@code name} (lower-cased, with
     *     runs of other characters replaced by {@code -}). Set the name you want the slug built from.
     */
    @Deprecated
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
