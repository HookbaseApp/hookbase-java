package app.hookbase.errors;

import java.util.Map;

/** HTTP 404: the resource does not exist or belongs to another organization. */
public final class NotFoundException extends HookbaseApiException {
    public NotFoundException(String message, String requestId, Map<String, Object> details) {
        super(message, 404, "not_found", requestId, details);
    }
}
