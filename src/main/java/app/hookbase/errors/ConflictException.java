package app.hookbase.errors;

import java.util.Map;

/** HTTP 409: the request conflicts with existing state, for example a duplicate slug. */
public final class ConflictException extends HookbaseApiException {
    public ConflictException(String message, String requestId, Map<String, Object> details) {
        super(message, 409, "conflict", requestId, details);
    }
}
