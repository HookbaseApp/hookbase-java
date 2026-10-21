package app.hookbase.resources;

import app.hookbase.ApiClient;
import app.hookbase.RequestOptions;
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

    public CronJobsResource(ApiClient http) {
        super(http);
    }

    /** Every cron job. The API returns them all at once. */
    public List<CronJob> list(RequestOptions options) {
        return list(get(PATH, null, options), "cronJobs", CronJob.class);
    }

    public List<CronJob> list() {
        return list((RequestOptions) null);
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

    /** Every cron group. The API returns them all at once. */
    public List<CronGroup> listGroups(RequestOptions options) {
        JsonNode node = get(GROUPS, null, options);
        return list(node, node != null && node.has("groups") ? "groups" : "data", CronGroup.class);
    }

    public List<CronGroup> listGroups() {
        return listGroups(null);
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
