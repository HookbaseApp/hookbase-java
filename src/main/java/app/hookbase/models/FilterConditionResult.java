package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One condition's outcome in a filter test.
 *
 * @param fieldValue the value found at {@code field} in the payload ({@code null} when absent)
 */
public record FilterConditionResult(String field, String operator, JsonNode value, JsonNode fieldValue, boolean passed) {}
