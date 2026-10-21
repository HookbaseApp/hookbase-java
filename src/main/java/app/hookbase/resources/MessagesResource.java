package app.hookbase.resources;

import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.MessageAttempt;
import app.hookbase.models.MessageExport;
import app.hookbase.models.MessageReplayResult;
import app.hookbase.models.MessageStatsSummary;
import app.hookbase.models.OutboundMessage;
import app.hookbase.models.SendMessageEndpoint;
import app.hookbase.models.SendMessageResponse;
import app.hookbase.params.ExportMessagesParams;
import app.hookbase.params.ListMessagesParams;
import app.hookbase.params.SendMessageParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Sending outbound webhooks, and the messages (one per endpoint per event) that deliver them. */
public final class MessagesResource extends BaseResource {

    private static final String SEND_PATH = "/api/send-event";
    private static final String PATH = "/api/outbound-messages";

    public MessagesResource(HttpTransport http) {
        super(http);
    }

    /**
     * Send an event to every endpoint subscribed to its type (or to {@code endpointIds} only).
     *
     * <p>Every call carries an idempotency key: {@code params.idempotencyKey}, else
     * {@code options.idempotencyKey}, else a UUID generated for this call. It goes in both the body
     * and the {@code Idempotency-Key} header, so the client's own retries after a timeout or 5xx
     * return the first send's result instead of sending the event again.
     *
     * @param applicationId the application to send for; left out of the body when {@code null} or empty
     */
    public SendMessageResponse send(String applicationId, SendMessageParams params, RequestOptions options) {
        LinkedHashMap<String, Object> body = body(params);
        Object endpointIds = body.remove("endpointIds");
        Object endpointId = body.remove("endpointId");
        body.remove("eventId"); // the API assigns it and never accepted one

        String key = params != null && params.get("idempotencyKey") instanceof String s && !s.isEmpty()
                ? s
                : options != null && options.idempotencyKey() != null && !options.idempotencyKey().isEmpty()
                        ? options.idempotencyKey()
                        : UUID.randomUUID().toString();
        if (applicationId != null && !applicationId.isEmpty()) body.put("applicationId", applicationId);
        body.put("idempotencyKey", key);

        Set<String> targets = new LinkedHashSet<>();
        if (endpointIds instanceof Collection<?> ids) {
            for (Object id : ids) if (id != null) targets.add(id.toString());
        }
        if (endpointId instanceof String id && !id.isEmpty()) targets.add(id);
        if (!targets.isEmpty()) {
            body.put("endpointIds", List.copyOf(targets));
            // The API before endpointIds support reads only the singular field; with exactly one
            // target, send it there too so the event is not fanned out to every subscriber.
            if (targets.size() == 1) body.put("endpointId", targets.iterator().next());
        }

        RequestOptions withKey = new RequestOptions(
                options == null ? null : options.timeout(), options == null ? null : options.retries(), key);
        JsonNode node = post(SEND_PATH, body, withKey);
        JsonNode data = node == null ? null : node.has("data") ? node.get("data") : node;
        List<SendMessageEndpoint> endpoints = list(data, "endpoints", SendMessageEndpoint.class);
        int queued = data != null && data.hasNonNull("messagesQueued") ? data.get("messagesQueued").asInt() : endpoints.size();
        String eventId = data != null && data.hasNonNull("eventId") ? data.get("eventId").asText() : null;
        return new SendMessageResponse(eventId, queued, endpoints);
    }

    public SendMessageResponse send(String applicationId, SendMessageParams params) {
        return send(applicationId, params, null);
    }

    /** One page of an application's outbound messages, newest first. Pass the returned {@link Page#cursor()} back as {@code cursor} for the next. */
    public Page<OutboundMessage> list(String applicationId, ListMessagesParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        q.put("applicationId", applicationId);
        return cursorPage(PATH, q, options, OutboundMessage.class, null);
    }

    public Page<OutboundMessage> list(String applicationId, ListMessagesParams params) {
        return list(applicationId, params, null);
    }

    public Page<OutboundMessage> list(String applicationId) {
        return list(applicationId, null, null);
    }

    /** Every outbound message of an application, fetched 100 at a time (or {@code limit}) as you iterate. */
    public Pager<OutboundMessage> listAll(String applicationId, ListMessagesParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        q.put("applicationId", applicationId);
        q.putIfAbsent("limit", 100);
        String start = (String) q.remove("cursor");
        return Pager.cursor(start, cursor -> {
            Map<String, Object> page = new LinkedHashMap<>(q);
            page.put("cursor", cursor);
            return cursorPage(PATH, page, options, OutboundMessage.class, null);
        });
    }

    public Pager<OutboundMessage> listAll(String applicationId, ListMessagesParams params) {
        return listAll(applicationId, params, null);
    }

    public Pager<OutboundMessage> listAll(String applicationId) {
        return listAll(applicationId, null, null);
    }

    /** One outbound message, with the detail fields (payload, metadata, ...) list items leave out. */
    public OutboundMessage get(String applicationId, String messageId, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(messageId), (Map<String, ?>) null, options), "data", OutboundMessage.class);
    }

    public OutboundMessage get(String applicationId, String messageId) {
        return get(applicationId, messageId, null);
    }

    /**
     * The same as {@link #list(String, ListMessagesParams, RequestOptions)}. The message list has no
     * event filter, so {@code messageId} does not narrow the result.
     */
    public Page<OutboundMessage> listOutbound(
            String applicationId, String messageId, ListMessagesParams params, RequestOptions options) {
        return list(applicationId, params, options);
    }

    public Page<OutboundMessage> listOutbound(String applicationId, String messageId, ListMessagesParams params) {
        return listOutbound(applicationId, messageId, params, null);
    }

    public Page<OutboundMessage> listOutbound(String applicationId, String messageId) {
        return listOutbound(applicationId, messageId, null, null);
    }

    /** The same as {@link #get(String, String, RequestOptions)}. */
    public OutboundMessage getOutbound(String applicationId, String outboundMessageId, RequestOptions options) {
        return get(applicationId, outboundMessageId, options);
    }

    public OutboundMessage getOutbound(String applicationId, String outboundMessageId) {
        return getOutbound(applicationId, outboundMessageId, null);
    }

    /**
     * The same as {@link #list(String, ListMessagesParams, RequestOptions)}: one page, despite the
     * name. Use {@link #listAll(String, ListMessagesParams, RequestOptions)} to iterate every message.
     */
    public Page<OutboundMessage> listAllOutbound(String applicationId, ListMessagesParams params, RequestOptions options) {
        return list(applicationId, params, options);
    }

    public Page<OutboundMessage> listAllOutbound(String applicationId, ListMessagesParams params) {
        return listAllOutbound(applicationId, params, null);
    }

    public Page<OutboundMessage> listAllOutbound(String applicationId) {
        return listAllOutbound(applicationId, null, null);
    }

    /** Every delivery attempt of an outbound message, oldest first. */
    public List<MessageAttempt> listAttempts(String applicationId, String outboundMessageId, RequestOptions options) {
        return list(get(PATH + "/" + seg(outboundMessageId) + "/attempts", (Map<String, ?>) null, options), "data", MessageAttempt.class);
    }

    public List<MessageAttempt> listAttempts(String applicationId, String outboundMessageId) {
        return listAttempts(applicationId, outboundMessageId, null);
    }

    /**
     * Replay an exhausted, DLQ or stalled failed outbound message. The replay is a NEW outbound
     * message, whose id is {@link MessageReplayResult#newMessageId()}.
     *
     * <p>Not retried automatically after a timeout or 5xx unless you set an idempotency key or
     * {@link RequestOptions#retries()}: a replay that did go through would otherwise be delivered
     * twice.
     */
    public MessageReplayResult retry(String applicationId, String outboundMessageId, RequestOptions options) {
        return unwrap(post(PATH + "/" + seg(outboundMessageId) + "/replay", null, options), "data", MessageReplayResult.class);
    }

    public MessageReplayResult retry(String applicationId, String outboundMessageId) {
        return retry(applicationId, outboundMessageId, null);
    }

    /** The same as {@link #retry(String, String, RequestOptions)}. */
    public MessageReplayResult resend(String applicationId, String messageId, RequestOptions options) {
        return retry(applicationId, messageId, options);
    }

    public MessageReplayResult resend(String applicationId, String messageId) {
        return resend(applicationId, messageId, null);
    }

    /** Outbound message counts by status, across the organization. */
    public MessageStatsSummary getStatsSummary(RequestOptions options) {
        return unwrap(get(PATH + "/stats/summary", (Map<String, ?>) null, options), "data", MessageStatsSummary.class);
    }

    public MessageStatsSummary getStatsSummary() {
        return getStatsSummary(null);
    }

    /** Export outbound events (the default) or messages as JSON. See {@link #exportCsv} for CSV. */
    public MessageExport export(ExportMessagesParams params, RequestOptions options) {
        Map<String, Object> q = query(params, "format");
        return as(get(PATH + "/export", q, options), MessageExport.class);
    }

    public MessageExport export(ExportMessagesParams params) {
        return export(params, null);
    }

    public MessageExport export() {
        return export(null, null);
    }

    /** Export outbound events (the default) or messages as CSV text. */
    public String exportCsv(ExportMessagesParams params, RequestOptions options) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("format", "csv");
        q.putAll(query(params, "format"));
        JsonNode node = get(PATH + "/export", q, options);
        return node == null ? "" : node.asText();
    }

    public String exportCsv(ExportMessagesParams params) {
        return exportCsv(params, null);
    }

    public String exportCsv() {
        return exportCsv(null, null);
    }
}
