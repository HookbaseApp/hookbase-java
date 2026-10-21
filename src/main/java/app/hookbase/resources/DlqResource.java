package app.hookbase.resources;

import app.hookbase.ApiClient;
import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.models.DlqBulkDeleteResult;
import app.hookbase.models.DlqBulkRetryResult;
import app.hookbase.models.DlqMessage;
import app.hookbase.models.DlqStats;
import app.hookbase.models.MessageReplayResult;
import app.hookbase.params.ListDlqParams;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The outbound dead letter queue: messages that ran out of retries or were moved there. */
public final class DlqResource extends BaseResource {

    private static final String PATH = "/api/outbound-messages/dlq";

    public DlqResource(ApiClient http) {
        super(http);
    }

    /** One page of DLQ messages. Pass the returned {@link Page#cursor()} back as {@code cursor} for the next. */
    public Page<DlqMessage> list(ListDlqParams params, RequestOptions options) {
        return cursorPage(PATH + "/messages", query(params), options, DlqMessage.class, null);
    }

    public Page<DlqMessage> list(ListDlqParams params) {
        return list(params, null);
    }

    public Page<DlqMessage> list() {
        return list(null, null);
    }

    /** Every DLQ message, fetched a page ({@code limit}, else the API's default) at a time as you iterate. */
    public Pager<DlqMessage> listAll(ListDlqParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        String start = (String) q.remove("cursor");
        return Pager.cursor(start, cursor -> {
            Map<String, Object> page = new LinkedHashMap<>(q);
            page.put("cursor", cursor);
            return cursorPage(PATH + "/messages", page, options, DlqMessage.class, null);
        });
    }

    public Pager<DlqMessage> listAll(ListDlqParams params) {
        return listAll(params, null);
    }

    public Pager<DlqMessage> listAll() {
        return listAll(null, null);
    }

    /** DLQ totals by reason, and the top endpoints and event types. */
    public DlqStats getStats(RequestOptions options) {
        return unwrap(get(PATH + "/stats", null, options), "data", DlqStats.class);
    }

    public DlqStats getStats() {
        return getStats(null);
    }

    /**
     * Retry one DLQ message. The retry is a new outbound message, whose id is
     * {@link MessageReplayResult#newMessageId()}. Not retried automatically after a timeout or 5xx
     * unless you set {@link RequestOptions#retries()}.
     */
    public MessageReplayResult retry(String id, RequestOptions options) {
        return unwrap(post(PATH + "/" + seg(id) + "/retry", null, options), "data", MessageReplayResult.class);
    }

    public MessageReplayResult retry(String id) {
        return retry(id, null);
    }

    /** Retry up to 100 DLQ messages. */
    public DlqBulkRetryResult retryBulk(List<String> messageIds, RequestOptions options) {
        return unwrap(post(PATH + "/retry-bulk", Map.of("messageIds", messageIds), options), "data", DlqBulkRetryResult.class);
    }

    public DlqBulkRetryResult retryBulk(List<String> messageIds) {
        return retryBulk(messageIds, null);
    }

    public void delete(String id, RequestOptions options) {
        delete(PATH + "/" + seg(id), null, options);
    }

    public void delete(String id) {
        delete(id, null);
    }

    /** Delete up to 100 DLQ messages. */
    public DlqBulkDeleteResult deleteBulk(List<String> messageIds, RequestOptions options) {
        return unwrap(delete(PATH + "/bulk", Map.of("messageIds", messageIds), options), "data", DlqBulkDeleteResult.class);
    }

    public DlqBulkDeleteResult deleteBulk(List<String> messageIds) {
        return deleteBulk(messageIds, null);
    }
}
