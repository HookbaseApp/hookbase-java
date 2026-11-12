package app.hookbase.models;

/**
 * Result of {@code destinations().test()}.
 *
 * <p>For a {@code bridge} destination the test is a dry run ({@code dryRun} is true): the API
 * resolves the event type a sample payload would be published as and counts the endpoints
 * subscribed to it, and publishes, writes and charges nothing. {@code success} is then whether that
 * event type exists.
 *
 * @param status the HTTP status the destination answered with; {@code null} when the request
 *     itself failed, and for a dry run
 * @param latencyMs how long the test request took
 * @param responseBody the first 1000 characters of the response
 * @param error set when the test failed (network error, missing warehouse configuration), or when
 *     a dry run could not resolve an event type from the payload
 * @param dryRun the test was a bridge dry run; false for every other destination type
 * @param eventType dry run only: the event type the payload resolves to, or {@code null} when none
 *     could be read from it
 * @param eventTypeExists dry run only: whether that event type exists and is enabled; {@code null}
 *     for any other test
 * @param matchingEndpoints dry run only: endpoints subscribed to the event type, counted before
 *     label filters, filter expressions and circuit state; {@code null} for any other test
 */
public record DestinationTestResult(
        boolean success,
        Integer status,
        Integer latencyMs,
        String responseBody,
        String error,
        boolean dryRun,
        String eventType,
        Boolean eventTypeExists,
        Integer matchingEndpoints) {

    /**
     * A result with no dry-run fields: the constructor this record had before bridge destinations,
     * kept so code that builds one (a test stub, say) still compiles.
     */
    public DestinationTestResult(boolean success, Integer status, Integer latencyMs, String responseBody, String error) {
        this(success, status, latencyMs, responseBody, error, false, null, null, null);
    }
}
