package app.hookbase.errors;

/** The request did not complete within its timeout. */
public final class HookbaseTimeoutException extends HookbaseException {
    public HookbaseTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
