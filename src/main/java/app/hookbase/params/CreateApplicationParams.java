package app.hookbase.params;

import java.util.Map;

/** Body for {@code applications().create()}. {@code name} is required. */
public final class CreateApplicationParams extends Params<CreateApplicationParams> {
    public CreateApplicationParams name(String name) {
        return set("name", name);
    }

    /** Your own identifier for this customer; look it up later with {@code getByUid}. */
    public CreateApplicationParams externalId(String externalId) {
        return set("externalId", externalId);
    }

    public CreateApplicationParams metadata(Map<String, ?> metadata) {
        return set("metadata", metadata);
    }
}
