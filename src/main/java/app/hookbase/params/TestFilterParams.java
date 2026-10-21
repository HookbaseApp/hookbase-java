package app.hookbase.params;

import java.util.List;

/** Body for {@code filters().test()}. {@code conditions} and {@code payload} are required. */
public final class TestFilterParams extends Params<TestFilterParams> {
    /** See {@link CreateFilterParams#conditions(List)}. */
    public TestFilterParams conditions(List<?> conditions) {
        return set("conditions", conditions);
    }

    /** {@code AND} (default) or {@code OR}. */
    public TestFilterParams logic(String logic) {
        return set("logic", logic);
    }

    /** The sample payload (any JSON-serializable value). */
    public TestFilterParams payload(Object payload) {
        return set("payload", payload);
    }
}
