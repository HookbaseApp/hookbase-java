package app.hookbase.errors;

/** The request failed below HTTP: DNS, connection refused or reset, TLS, or interruption. */
public final class HookbaseNetworkException extends HookbaseException {
    public HookbaseNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
