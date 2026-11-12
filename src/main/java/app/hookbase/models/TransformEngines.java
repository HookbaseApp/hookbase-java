package app.hookbase.models;

import java.util.List;

/**
 * What {@code transforms().engines()} returns: the transform languages, and the content formats a
 * transform's {@code inputFormat} and {@code outputFormat} take.
 *
 * @param engines every transform language, available on this deployment or not; see
 *     {@link TransformEngine#available()}
 * @param contentFormats {@code json}, {@code xml} and {@code text} today
 */
public record TransformEngines(List<TransformEngine> engines, List<String> contentFormats) {
    public TransformEngines {
        engines = engines == null ? List.of() : List.copyOf(engines);
        contentFormats = contentFormats == null ? List.of() : List.copyOf(contentFormats);
    }
}
