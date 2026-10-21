package app.hookbase.errors;

import java.util.Map;

/** HTTP 401: the API key is missing, malformed, revoked or expired. */
public final class AuthenticationException extends HookbaseApiException {
    public AuthenticationException(String message, String requestId, Map<String, Object> details) {
        super(message, 401, "authentication_error", requestId, details);
    }
}
