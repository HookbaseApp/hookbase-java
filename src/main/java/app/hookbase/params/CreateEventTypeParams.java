package app.hookbase.params;

/** Body for {@code eventTypes().create()}. {@code name} is required. */
public final class CreateEventTypeParams extends Params<CreateEventTypeParams> {
    /** Lowercase, optionally dot-separated (e.g. {@code order.created}), max 100 characters. */
    public CreateEventTypeParams name(String name) {
        return set("name", name);
    }

    public CreateEventTypeParams displayName(String displayName) {
        return set("displayName", displayName);
    }

    public CreateEventTypeParams description(String description) {
        return set("description", description);
    }

    public CreateEventTypeParams category(String category) {
        return set("category", category);
    }

    /**
     * JSON Schema for the payload: a {@code Map} (or anything Jackson serializes), sent as the JSON
     * string the API takes, or a {@code String} already holding the JSON, sent as it is.
     */
    public CreateEventTypeParams schema(Object schema) {
        return set("schema", schema);
    }

    /** Example payload; converted to a JSON string the same way as {@link #schema(Object)}. */
    public CreateEventTypeParams examplePayload(Object examplePayload) {
        return set("examplePayload", examplePayload);
    }

    public CreateEventTypeParams documentationUrl(String documentationUrl) {
        return set("documentationUrl", documentationUrl);
    }

    /** The API defaults to true. */
    public CreateEventTypeParams isEnabled(boolean isEnabled) {
        return set("isEnabled", isEnabled);
    }

    /** 0=Critical, 1=High, 2=Normal, 3=Low, 4=Bulk. */
    public CreateEventTypeParams defaultPriority(int defaultPriority) {
        return set("defaultPriority", defaultPriority);
    }
}
