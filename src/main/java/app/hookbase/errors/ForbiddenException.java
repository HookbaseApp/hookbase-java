package app.hookbase.errors;

import java.util.Map;

/** HTTP 403: the key is valid but lacks the scope, role or plan for this request. */
public final class ForbiddenException extends HookbaseApiException {
    public ForbiddenException(String message, String requestId, Map<String, Object> details) {
        super(message, 403, "forbidden", requestId, details);
    }
}
