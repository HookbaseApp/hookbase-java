package app.hookbase.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One warehouse column mapping, as returned and as sent with {@code fieldMapping(...)}.
 *
 * @param source JSONPath into the event, e.g. {@code $.id}
 * @param target the column name
 * @param type {@code string}, {@code number}, {@code boolean}, {@code timestamp} or {@code json}
 * @param defaultValue the value used when {@code source} is absent (JSON key {@code default})
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DestinationFieldMapping(
        String source, String target, String type, @JsonProperty("default") String defaultValue) {

    public DestinationFieldMapping(String source, String target, String type) {
        this(source, target, type, null);
    }
}
