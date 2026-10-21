package app.hookbase.models;

import java.util.List;

/**
 * A JSON export from {@code messages().export()}. {@code events} is set for type {@code events} (the
 * default) and {@code messages} for type {@code messages}; the other is {@code null}.
 *
 * @param organization the organization's slug
 */
public record MessageExport(
        String exportedAt,
        String organization,
        int count,
        List<MessageExportEvent> events,
        List<MessageExportMessage> messages) {}
