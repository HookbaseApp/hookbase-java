package app.hookbase.errors;

/** A webhook's headers or signature did not verify. Respond 400/401 and do not process it. */
public final class WebhookVerificationException extends HookbaseException {
    public WebhookVerificationException(String message) {
        super(message);
    }

    public WebhookVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
