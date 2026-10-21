package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The result of {@code transforms().test()}. A transform that fails to run is reported here with
 * {@code success == false}, not as an exception.
 *
 * @param result the transform's output: parsed JSON for JSON output, otherwise a string
 * @param contentType the output's content type
 * @param error why the transform failed
 */
public record TransformTestResult(boolean success, JsonNode result, String contentType, String error) {}
