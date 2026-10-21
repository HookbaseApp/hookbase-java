package app.hookbase.errors;

import java.util.List;
import java.util.Map;

/** HTTP 400 or 422: the request body or query failed validation. */
public final class ValidationException extends HookbaseApiException {
    private final Map<String, List<String>> fieldErrors;

    public ValidationException(
            String message,
            int status,
            String requestId,
            Map<String, Object> details,
            Map<String, List<String>> fieldErrors) {
        super(message, status, "validation_error", requestId, details);
        this.fieldErrors = fieldErrors == null ? Map.of() : fieldErrors;
    }

    /** Per-field messages, keyed by field name; empty when the API sent none. */
    public Map<String, List<String>> fieldErrors() {
        return fieldErrors;
    }
}
