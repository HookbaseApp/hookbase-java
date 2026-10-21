package app.hookbase.params;

/**
 * Body for {@code eventTypes().update()}. Only the fields you set are changed; {@code category},
 * {@code schema}, {@code examplePayload}, {@code documentationUrl} and {@code defaultPriority} can be
 * set to {@code null} to clear them.
 */
public final class UpdateEventTypeParams extends Params<UpdateEventTypeParams> {
    public UpdateEventTypeParams displayName(String displayName) {
        return set("displayName", displayName);
    }

    public UpdateEventTypeParams description(String description) {
        return set("description", description);
    }

    public UpdateEventTypeParams category(String category) {
        return set("category", category);
    }

    /** JSON Schema for the payload; see {@link CreateEventTypeParams#schema(Object)}. */
    public UpdateEventTypeParams schema(Object schema) {
        return set("schema", schema);
    }

    /** Example payload; see {@link CreateEventTypeParams#examplePayload(Object)}. */
    public UpdateEventTypeParams examplePayload(Object examplePayload) {
        return set("examplePayload", examplePayload);
    }

    public UpdateEventTypeParams documentationUrl(String documentationUrl) {
        return set("documentationUrl", documentationUrl);
    }

    public UpdateEventTypeParams isEnabled(boolean isEnabled) {
        return set("isEnabled", isEnabled);
    }

    public UpdateEventTypeParams isDeprecated(boolean isDeprecated) {
        return set("isDeprecated", isDeprecated);
    }

    /** Max 500 characters. */
    public UpdateEventTypeParams deprecatedMessage(String deprecatedMessage) {
        return set("deprecatedMessage", deprecatedMessage);
    }

    public UpdateEventTypeParams isPublic(boolean isPublic) {
        return set("isPublic", isPublic);
    }

    /** 0=Critical, 1=High, 2=Normal, 3=Low, 4=Bulk. */
    public UpdateEventTypeParams defaultPriority(Integer defaultPriority) {
        return set("defaultPriority", defaultPriority);
    }
}
