package app.hookbase.models;

import java.util.List;

/** The result of {@code filters().test()}: whether the payload matched, and each condition's outcome. */
public record FilterTestResult(boolean matches, List<FilterConditionResult> results, String logic) {}
