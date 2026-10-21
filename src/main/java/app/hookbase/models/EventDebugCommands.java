package app.hookbase.models;

import app.hookbase.internal.Redact;
import java.util.Map;

/** Ready-to-run commands for an event. {@code curlCommand} is {@code null} for a transient event. */
public record EventDebugCommands(String curlCommand) {
    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("curlCommand", Redact.Rule.SECRET));
    }
}
