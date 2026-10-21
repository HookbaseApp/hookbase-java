package app.hookbase.errors;

/**
 * Base class of every exception the SDK throws. Unchecked, like the other Hookbase SDKs' errors.
 *
 * <p>Thrown directly only for client-side problems that are not an API response, a timeout, a
 * network failure or a webhook verification failure (for example, a destination name with nothing
 * to derive a slug from).
 */
public sealed class HookbaseException extends RuntimeException
        permits HookbaseApiException, HookbaseTimeoutException, HookbaseNetworkException,
                WebhookVerificationException {

    public HookbaseException(String message) {
        super(message);
    }

    public HookbaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
