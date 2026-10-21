package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One filter condition: {@code field} (a dot path into the payload) compared with {@code value}
 * using {@code operator} (e.g. {@code equals}, {@code not_equals}, {@code contains},
 * {@code greater_than}, {@code exists}).
 */
public record FilterCondition(String field, String operator, JsonNode value) {}
