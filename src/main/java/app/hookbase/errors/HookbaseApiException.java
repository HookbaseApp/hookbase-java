package app.hookbase.errors;

import java.util.Map;

/**
 * The API answered with an error status. Statuses with a dedicated subclass (401, 403, 404, 409,
 * 400/422, 429) are always thrown as that subclass; anything else (5xx, other 4xx) as this class.
 */
public sealed class HookbaseApiException extends HookbaseException
        permits AuthenticationException, ForbiddenException, NotFoundException, ConflictException,
                ValidationException, RateLimitException {

    private final int status;
    private final String code;
    private final String requestId;
    private final Map<String, Object> details;

    public HookbaseApiException(
            String message, int status, String code, String requestId, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.requestId = requestId;
        this.details = details == null ? Map.of() : details;
    }

    /** HTTP status code. */
    public int status() {
        return status;
    }

    /** Machine-readable error code from the response body, or a default per status. */
    public String code() {
        return code;
    }

    /** The {@code x-request-id} response header, or {@code null}. Quote it in support requests. */
    public String requestId() {
        return requestId;
    }

    /** The response body's {@code details} object; empty when there was none. */
    public Map<String, Object> details() {
        return details;
    }
}
