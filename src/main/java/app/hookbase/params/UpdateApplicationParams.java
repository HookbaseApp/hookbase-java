package app.hookbase.params;

import java.util.Map;

/** Body for {@code applications().update()}. Only the fields you set are changed; set one to {@code null} to clear it. */
public final class UpdateApplicationParams extends Params<UpdateApplicationParams> {
    public UpdateApplicationParams name(String name) {
        return set("name", name);
    }

    public UpdateApplicationParams metadata(Map<String, ?> metadata) {
        return set("metadata", metadata);
    }
}
