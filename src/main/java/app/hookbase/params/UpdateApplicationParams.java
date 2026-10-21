package app.hookbase.params;

import java.util.Map;

/** Body for {@code applications().update()}. Only the fields you set are changed. The API rejects {@code null} for every field, so none of them can be cleared. */
public final class UpdateApplicationParams extends Params<UpdateApplicationParams> {
    public UpdateApplicationParams name(String name) {
        return set("name", name);
    }

    public UpdateApplicationParams metadata(Map<String, ?> metadata) {
        return set("metadata", metadata);
    }
}
