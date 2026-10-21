package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * The result of {@code schemas().validate()}.
 *
 * @param payload the payload that was validated, echoed back
 */
public record SchemaValidationResult(boolean valid, List<SchemaValidationError> errors, JsonNode payload) {}
