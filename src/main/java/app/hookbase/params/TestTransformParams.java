package app.hookbase.params;

/** Body for {@code transforms().test()}. {@code code} and {@code payload} are required. */
public final class TestTransformParams extends Params<TestTransformParams> {
    /** {@code jsonata} (default), {@code liquid}, {@code xslt} or {@code javascript}. */
    public TestTransformParams transformType(String transformType) {
        return set("transformType", transformType);
    }

    public TestTransformParams code(String code) {
        return set("code", code);
    }

    public TestTransformParams inputFormat(String inputFormat) {
        return set("inputFormat", inputFormat);
    }

    public TestTransformParams outputFormat(String outputFormat) {
        return set("outputFormat", outputFormat);
    }

    /** The sample payload: any JSON-serializable value, or a string sent as the raw body. */
    public TestTransformParams payload(Object payload) {
        return set("payload", payload);
    }
}
