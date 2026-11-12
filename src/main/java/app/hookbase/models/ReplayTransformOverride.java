package app.hookbase.models;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A one-off transform for {@code deliveries().replay()} to run instead of the route's own
 * ({@link app.hookbase.params.ReplayDeliveryParams#transformOverride}). It is checked like a new
 * transform (plan, language, formats, compilation) and is not saved unless
 * {@code persistTransform} is set. Unset components are left out of the request.
 *
 * @param code the transform code
 * @param type the language, as {@link TransformEngine#type()} ({@code jsonata}, {@code javascript},
 *     ...); {@code null} for the API's default
 * @param inputFormat {@code json}, {@code xml} or {@code text}; {@code null} for the default
 * @param outputFormat {@code json}, {@code xml} or {@code text}; {@code null} for the default
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReplayTransformOverride(String code, String type, String inputFormat, String outputFormat) {

    /** An override in {@code type} that reads and writes the default formats. */
    public ReplayTransformOverride(String code, String type) {
        this(code, type, null, null);
    }
}
