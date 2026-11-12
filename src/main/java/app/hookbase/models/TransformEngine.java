package app.hookbase.models;

/**
 * A transform language the API supports, from {@code transforms().engines()}.
 *
 * @param type the value to pass as a transform's {@code transformType}, e.g. {@code jsonata},
 *     {@code javascript}, {@code jq}
 * @param label a display name, e.g. {@code JSONata}
 * @param description one line on what the language is for
 * @param fileExtension the extension a file of this language's code takes, e.g. {@code .jsonata}
 * @param starterCode a small working transform to start from
 * @param aiGenerate whether the dashboard can generate code in this language
 * @param available whether this deployment runs the language; false when it needs transform
 *     isolation and that is off, in which case a transform in it is refused
 */
public record TransformEngine(
        String type,
        String label,
        String description,
        String fileExtension,
        String starterCode,
        boolean aiGenerate,
        boolean available) {}
