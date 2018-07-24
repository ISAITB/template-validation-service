package ${package}.validation;

/**
 * Simple class to represent the result of a validation.
 */
public class ValidationResult {

    private boolean textsMatch;
    private boolean textsMatchCaseInsensitive;

    /**
     * Constructor.
     *
     * @param textsMatch Whether or not the texts match.
     * @param textsMatchCaseInsensitive Whether or not the texts match also in a case-insensitive manner.
     */
    public ValidationResult(boolean textsMatch, boolean textsMatchCaseInsensitive) {
        this.textsMatch = textsMatch;
        this.textsMatchCaseInsensitive = textsMatchCaseInsensitive;
    }

    /**
     * Check if the texts matched.
     *
     * @return The check result.
     */
    public boolean isTextsMatch() {
        return textsMatch;
    }

    /**
     * Check if the texts match in a case-insensitive manner.
     *
     * @return The check result.
     */
    public boolean isTextsMatchCaseInsensitive() {
        return textsMatchCaseInsensitive;
    }
}
