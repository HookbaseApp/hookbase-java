package app.hookbase.resources;

import app.hookbase.ApiClient;
import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.errors.NotFoundException;
import app.hookbase.internal.Json;
import app.hookbase.models.EventType;
import app.hookbase.params.CreateEventTypeParams;
import app.hookbase.params.ListEventTypesParams;
import app.hookbase.params.UpdateEventTypeParams;
import java.util.LinkedHashMap;
import java.util.Map;

/** Outbound event types, such as {@code order.created}, that endpoints subscribe to. */
public final class EventTypesResource extends BaseResource {

    private static final String PATH = "/api/event-types";

    public EventTypesResource(ApiClient http) {
        super(http);
    }

    /** One page of event types. Pass the returned {@link Page#cursor()} back as {@code cursor} for the next. */
    public Page<EventType> list(ListEventTypesParams params, RequestOptions options) {
        return cursorPage(PATH, query(params), options, EventType.class, null);
    }

    public Page<EventType> list(ListEventTypesParams params) {
        return list(params, null);
    }

    public Page<EventType> list() {
        return list(null, null);
    }

    /** Every event type, fetched 100 at a time (or {@code limit}) as you iterate. */
    public Pager<EventType> listAll(ListEventTypesParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        q.putIfAbsent("limit", 100);
        String start = (String) q.remove("cursor");
        return Pager.cursor(start, cursor -> {
            Map<String, Object> page = new LinkedHashMap<>(q);
            page.put("cursor", cursor);
            return cursorPage(PATH, page, options, EventType.class, null);
        });
    }

    public Pager<EventType> listAll(ListEventTypesParams params) {
        return listAll(params, null);
    }

    public Pager<EventType> listAll() {
        return listAll(null, null);
    }

    public EventType get(String id, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(id), null, options), "data", EventType.class);
    }

    public EventType get(String id) {
        return get(id, null);
    }

    /**
     * The event type with exactly this name. The API's {@code search} is a substring match, so this
     * pages through every match ({@code order} also matches {@code order.created}, ...) until it finds
     * the exact one.
     *
     * @throws NotFoundException if there is none
     */
    public EventType getByName(String name, RequestOptions options) {
        for (EventType et : listAll(new ListEventTypesParams().search(name).limit(100), options)) {
            if (name.equals(et.name())) return et;
        }
        throw new NotFoundException("Event type '" + name + "' not found", null, null);
    }

    public EventType getByName(String name) {
        return getByName(name, null);
    }

    /** Create an event type. {@code schema} and {@code examplePayload} are sent as the JSON strings the API takes. */
    public EventType create(CreateEventTypeParams params, RequestOptions options) {
        return unwrap(post(PATH, jsonTextBody(body(params)), options), "data", EventType.class);
    }

    public EventType create(CreateEventTypeParams params) {
        return create(params, null);
    }

    public EventType update(String id, UpdateEventTypeParams params, RequestOptions options) {
        return unwrap(patch(PATH + "/" + seg(id), jsonTextBody(body(params)), options), "data", EventType.class);
    }

    public EventType update(String id, UpdateEventTypeParams params) {
        return update(id, params, null);
    }

    public void delete(String id, RequestOptions options) {
        delete(PATH + "/" + seg(id), null, options);
    }

    public void delete(String id) {
        delete(id, null);
    }

    /** Archive the event type (an update that sends only {@code isEnabled: false}). */
    public EventType archive(String id, RequestOptions options) {
        return update(id, new UpdateEventTypeParams().isEnabled(false), options);
    }

    public EventType archive(String id) {
        return archive(id, null);
    }

    /** Unarchive the event type (an update that sends only {@code isEnabled: true}). */
    public EventType unarchive(String id, RequestOptions options) {
        return update(id, new UpdateEventTypeParams().isEnabled(true), options);
    }

    public EventType unarchive(String id) {
        return unarchive(id, null);
    }

    /**
     * The event type named {@code name}, created from {@code params} if there is none.
     * {@code params}' own name, if set, is overridden.
     */
    public EventType getOrCreate(String name, CreateEventTypeParams params, RequestOptions options) {
        try {
            return getByName(name, options);
        } catch (NotFoundException e) {
            LinkedHashMap<String, Object> body = body(params);
            body.put("name", name);
            return unwrap(post(PATH, jsonTextBody(body), options), "data", EventType.class);
        }
    }

    public EventType getOrCreate(String name, CreateEventTypeParams params) {
        return getOrCreate(name, params, null);
    }

    /** The API takes {@code schema} and {@code examplePayload} as JSON strings; a string is taken to be JSON already. */
    private static LinkedHashMap<String, Object> jsonTextBody(LinkedHashMap<String, Object> body) {
        for (String key : new String[] {"schema", "examplePayload"}) {
            Object value = body.get(key);
            if (value != null && !(value instanceof CharSequence)) body.put(key, Json.write(value));
        }
        return body;
    }
}
