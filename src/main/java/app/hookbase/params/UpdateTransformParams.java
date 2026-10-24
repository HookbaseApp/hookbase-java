package app.hookbase.params;

/** Body for {@code transforms().update()}. Only the fields you set are changed; set {@code description} to {@code null} to clear it. */
public final class UpdateTransformParams extends Params<UpdateTransformParams> {
    public UpdateTransformParams name(String name) {
        return set("name", name);
    }

    public UpdateTransformParams description(String description) {
        return set("description", description);
    }

    /** {@code jsonata}, {@code liquid}, {@code xslt} or {@code javascript}. */
    public UpdateTransformParams transformType(String transformType) {
        return set("transformType", transformType);
    }

    public UpdateTransformParams code(String code) {
        return set("code", code);
    }

    public UpdateTransformParams inputFormat(String inputFormat) {
        return set("inputFormat", inputFormat);
    }

    public UpdateTransformParams outputFormat(String outputFormat) {
        return set("outputFormat", outputFormat);
    }

    /** Turn the transform on or off. */
    public UpdateTransformParams isActive(boolean isActive) {
        return set("isActive", isActive);
    }
}
