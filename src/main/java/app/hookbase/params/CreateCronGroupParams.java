package app.hookbase.params;

/** Body for {@code cronJobs().createGroup()}. {@code name} is required and must be unique. */
public final class CreateCronGroupParams extends Params<CreateCronGroupParams> {
    public CreateCronGroupParams name(String name) {
        return set("name", name);
    }

    public CreateCronGroupParams description(String description) {
        return set("description", description);
    }
}
