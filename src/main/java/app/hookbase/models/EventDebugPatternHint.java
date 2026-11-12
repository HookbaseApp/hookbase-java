package app.hookbase.models;

/**
 * A known failure pattern a failed delivery's response matches: a starting point for finding the
 * cause ({@link EventDebugDelivery#patternHints()}).
 *
 * @param id the pattern's stable id, e.g. {@code signature-invalid}
 * @param category e.g. {@code auth_signature}
 * @param title a one-line summary
 * @param hypothesis what probably went wrong
 * @param suggestedFix what to check or change
 */
public record EventDebugPatternHint(String id, String category, String title, String hypothesis, String suggestedFix) {}
