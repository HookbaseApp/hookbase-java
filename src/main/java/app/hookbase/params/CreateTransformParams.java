package app.hookbase.params;

/** Body for {@code transforms().create()}. {@code name} and {@code code} are required. */
public final class CreateTransformParams extends Params<CreateTransformParams> {
    public CreateTransformParams name(String name) {
        return set("name", name);
    }

    /** Derived from the name when left out. */
    public CreateTransformParams slug(String slug) {
        return set("slug", slug);
    }

    public CreateTransformParams description(String description) {
        return set("description", description);
    }

    /** {@code jsonata} (default), {@code liquid}, {@code xslt} or {@code javascript}. */
    public CreateTransformParams transformType(String transformType) {
        return set("transformType", transformType);
    }

    public CreateTransformParams code(String code) {
        return set("code", code);
    }

    /** {@code json} (default), {@code xml} or {@code text}. */
    public CreateTransformParams inputFormat(String inputFormat) {
        return set("inputFormat", inputFormat);
    }

    /** {@code json} (default), {@code xml} or {@code text}. */
    public CreateTransformParams outputFormat(String outputFormat) {
        return set("outputFormat", outputFormat);
    }
}
