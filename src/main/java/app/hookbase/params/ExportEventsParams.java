package app.hookbase.params;

/** Filters for {@code events().export()} / {@code exportCsv()}. */
public final class ExportEventsParams extends Params<ExportEventsParams> {
    /** {@code json} (default) or {@code csv}. */
    public ExportEventsParams format(String format) {
        return set("format", format);
    }

    /** A source id or slug. One that matches no source is a 404. */
    public ExportEventsParams sourceId(String sourceId) {
        return set("sourceId", sourceId);
    }

    public ExportEventsParams eventType(String eventType) {
        return set("eventType", eventType);
    }

    public ExportEventsParams search(String search) {
        return set("search", search);
    }

    public ExportEventsParams fromDate(String fromDate) {
        return set("fromDate", fromDate);
    }

    public ExportEventsParams toDate(String toDate) {
        return set("toDate", toDate);
    }

    public ExportEventsParams signatureValid(boolean signatureValid) {
        return set("signatureValid", signatureValid ? "1" : "0");
    }

    /** {@code delivered}, {@code failed}, {@code pending} or {@code partial}. */
    public ExportEventsParams status(String status) {
        return set("status", status);
    }
}
