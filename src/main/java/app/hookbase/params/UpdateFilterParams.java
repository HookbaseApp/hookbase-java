package app.hookbase.params;

import java.util.List;

/** Body for {@code filters().update()}. Only the fields you set are changed; set {@code description} to {@code null} to clear it. */
public final class UpdateFilterParams extends Params<UpdateFilterParams> {
    public UpdateFilterParams name(String name) {
        return set("name", name);
    }

    public UpdateFilterParams description(String description) {
        return set("description", description);
    }

    /** Replaces all conditions; see {@link CreateFilterParams#conditions(List)}. */
    public UpdateFilterParams conditions(List<?> conditions) {
        return set("conditions", conditions);
    }

    /** {@code AND} or {@code OR}. */
    public UpdateFilterParams logic(String logic) {
        return set("logic", logic);
    }
}
