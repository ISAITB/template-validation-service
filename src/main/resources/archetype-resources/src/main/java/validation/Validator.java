package ${package}.validation;

import org.springframework.stereotype.Component;

/**
 * Class that implements the validator's logic irrespective of the API used.
 *
 * This is a sample implementation to simply encapsulate the validation logic in a reusable
 * manner. This result from this object is what is considered to construct the GITB-specific types used
 * in the service's response.
 *
 * For this example the validation logic simply checks if the provided string matches the expected value. In
 * addition it reports whether they match in a case-insensitive manner.
 */
@Component
public class Validator {

    /**
     * Validate the input.
     *
     * @param input The input string to check.
     * @param expected The expected value.
     * @return The result of the validation.
     */
    public ValidationResult validate(String input, String expected) {
        boolean textsMatch = input.equals(expected);
        return new ValidationResult(textsMatch, (textsMatch) || (input.equalsIgnoreCase(expected)));
    }

}
