package app.hookbase.resources;

import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.CronGroup;
import app.hookbase.models.CronJob;
import app.hookbase.models.CronJobExecution;
import app.hookbase.params.CreateCronGroupParams;
import app.hookbase.params.CreateCronJobParams;
import app.hookbase.params.UpdateCronJobParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** Cron jobs (scheduled HTTP requests) and their groups. */
public final class CronJobsResource extends BaseResource {

    private static final String PATH = "/api/cron";
    private static final String GROUPS = "/api/cron-groups";

    public CronJobsResource(HttpTransport http) {
        super(http);
    }

    /** Every cron job. The API returns them all at once, so this is a single page. */
    public Page<CronJob> list(RequestOptions options) {
        List<CronJob> data = list(get(PATH, null, options), "cronJobs", CronJob.class);
        return new Page<>(data, data.size(), data.size(), 0, false, null);
    }

    public Page<CronJob> list() {
        return list((RequestOptions) null);
    }

    /** Every cron job, as a {@link Pager} (one request). */
    public Pager<CronJob> listAll(RequestOptions options) {
        return Pager.numbered(1, n -> list(options));
    }

    public Pager<CronJob> listAll() {
        return listAll(null);
    }

    public CronJob get(String id, RequestOptions options) {
        return job(get(PATH + "/" + seg(id), null, options));
    }

    public CronJob get(String id) {
        return get(id, null);
    }

    /** Create a cron job. The response carries only the fields that were set; {@link #get} reads the rest. */
    public CronJob create(CreateCronJobParams params, RequestOptions options) {
        return job(post(PATH, body(params), options));
    }

    public CronJob create(CreateCronJobParams params) {
        return create(params, null);
    }

    public void update(String id, UpdateCronJobParams params, RequestOptions options) {
        patch(PATH + "/" + seg(id), body(params), options);
    }

    public void update(String id, UpdateCronJobParams params) {
        update(id, params, null);
    }

    public void delete(String id, RequestOptions options) {
        delete(PATH + "/" + seg(id), null, options);
    }

    public void delete(String id) {
        delete(id, null);
    }

    /**
     * Run a cron job now, outside its schedule. Counts as a monthly event on free plans the same as
     * a scheduled run, and honours the job's {@code useStaticIp} setting.
     */
    public CronJobExecution trigger(String id, RequestOptions options) {
        return unwrap(post(PATH + "/" + seg(id) + "/trigger", null, options), "execution", CronJobExecution.class);
    }

    public CronJobExecution trigger(String id) {
        return trigger(id, null);
    }

    /** Every cron group, as a single page. */
    public Page<CronGroup> listGroups(RequestOptions options) {
        JsonNode node = get(GROUPS, null, options);
        List<CronGroup> data = list(node, node != null && node.has("groups") ? "groups" : "data", CronGroup.class);
        return new Page<>(data, data.size(), data.size(), 0, false, null);
    }

    public Page<CronGroup> listGroups() {
        return listGroups(null);
    }

    /** Every cron group, as a {@link Pager} (one request). */
    public Pager<CronGroup> listAllGroups(RequestOptions options) {
        return Pager.numbered(1, n -> listGroups(options));
    }

    public Pager<CronGroup> listAllGroups() {
        return listAllGroups(null);
    }

    public CronGroup createGroup(CreateCronGroupParams params, RequestOptions options) {
        JsonNode node = post(GROUPS, body(params), options);
        return unwrap(node, node != null && node.has("group") ? "group" : "data", CronGroup.class);
    }

    public CronGroup createGroup(CreateCronGroupParams params) {
        return createGroup(params, null);
    }

    private static CronJob job(JsonNode node) {
        return unwrap(node, node != null && node.has("cronJob") ? "cronJob" : "data", CronJob.class);
    }
}
