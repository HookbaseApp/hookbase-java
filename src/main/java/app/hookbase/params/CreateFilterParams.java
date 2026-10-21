package app.hookbase.params;

import java.util.List;

/** Body for {@code filters().create()}. {@code name} and {@code conditions} are required. */
public final class CreateFilterParams extends Params<CreateFilterParams> {
    public CreateFilterParams name(String name) {
        return set("name", name);
    }

    /** Derived from the name when left out. */
    public CreateFilterParams slug(String slug) {
        return set("slug", slug);
    }

    public CreateFilterParams description(String description) {
        return set("description", description);
    }

    /**
     * The conditions: {@link app.hookbase.models.FilterCondition}s or maps with {@code field},
     * {@code operator} and {@code value}.
     */
    public CreateFilterParams conditions(List<?> conditions) {
        return set("conditions", conditions);
    }

    /** {@code AND} (default) or {@code OR}. */
    public CreateFilterParams logic(String logic) {
        return set("logic", logic);
    }
}
