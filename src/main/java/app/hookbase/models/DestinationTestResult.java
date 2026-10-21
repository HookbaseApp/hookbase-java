package app.hookbase.models;

/**
 * Result of {@code destinations().test()}.
 *
 * @param status the HTTP status the destination answered with; {@code null} when the request
 *     itself failed
 * @param latencyMs how long the test request took
 * @param responseBody the first 1000 characters of the response
 * @param error set when the test failed (network error, missing warehouse configuration)
 */
public record DestinationTestResult(boolean success, Integer status, Integer latencyMs, String responseBody, String error) {}
