#set($dollar = '$')
package ${package}.gitb;

import com.gitb.core.*;
import com.gitb.tr.*;
import com.gitb.tr.ObjectFactory;
import com.gitb.vs.*;
import com.gitb.vs.Void;
import ${package}.validation.ValidationResult;
import ${package}.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Spring component that realises the validation service.
 */
@Component
public class ValidationServiceImpl implements ValidationService {

    /** Logger. **/
    private static final Logger LOG = LoggerFactory.getLogger(ValidationServiceImpl.class);

    /** The name of the input parameter for the text to test. */
    public static final String INPUT__TEXT_TO_CHECK = "text";
    /** The name of the input parameter for the expected text. */
    public static final String INPUT__EXPECTED_TEXT = "expected";
    /** The name of the input parameter to determine whether a mismatch is an error or a warning. */
    public static final String INPUT__MISMATCH_IS_ERROR = "mismatchIsError";

    @Value("${dollar}{service.id}")
    private String serviceId;

    @Value("${dollar}{service.version}")
    private String serviceVersion;

    @Autowired
    private ObjectFactory objectFactory;

    @Autowired
    private Validator validator;

    /**
     * The purpose of the getModuleDefinition call is to inform its caller on how the service is supposed to be called.
     *
     * In this case its main purpose is to define the input parameters that are expected:
     * <ul>
     *     <li>The required input text (string).</li>
     *     <li>The required expected text (string).</li>
     *     <li>The optional flag to determine if a mismatch is an error (boolean).</li>
     * </ul>
     *
     * @param parameters No parameters are expected.
     * @return The response.
     */
    @Override
    public GetModuleDefinitionResponse getModuleDefinition(Void parameters) {
        GetModuleDefinitionResponse response = new GetModuleDefinitionResponse();
        response.setModule(new ValidationModule());
        response.getModule().setId(serviceId);
        response.getModule().setOperation("V");
        response.getModule().setMetadata(new Metadata());
        response.getModule().getMetadata().setName(response.getModule().getId());
        response.getModule().getMetadata().setVersion(serviceVersion);
        response.getModule().setInputs(new TypedParameters());
        response.getModule().getInputs().getParam().add(createParameter(INPUT__TEXT_TO_CHECK, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The text to check."));
        response.getModule().getInputs().getParam().add(createParameter(INPUT__EXPECTED_TEXT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The expected value for the provided text."));
        response.getModule().getInputs().getParam().add(createParameter(INPUT__MISMATCH_IS_ERROR, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether or not a mismatch should result in an error (true - the default) or a warning (false)."));
        return response;
    }

    /**
     * Create a parameter definition.
     *
     * @param name The name of the parameter.
     * @param type The type of the parameter. This needs to match one of the GITB types.
     * @param use The use (required or optional).
     * @param kind The kind og parameter it is (whether it should be provided as the specific value, as BASE64 content or as a URL that needs to be looked up to obtain the value).
     * @param description The description of the parameter.
     * @return The created parameter.
     */
    private TypedParameter createParameter(String name, String type, UsageEnumeration use, ConfigurationType kind, String description) {
        TypedParameter parameter =  new TypedParameter();
        parameter.setName(name);
        parameter.setType(type);
        parameter.setUse(use);
        parameter.setKind(kind);
        parameter.setDesc(description);
        return parameter;
    }

    /**
     * The validate operation is called to validate the input and produce a validation report.
     *
     * The expected input is described for the service's client through the getModuleDefinition call.
     *
     * @param parameters The input parameters and configuration for the validation.
     * @return The response containing the validation report.
     */
    @Override
    public ValidationResponse validate(ValidateRequest parameters) {
        ValidationResponse result = new ValidationResponse();
        // First extract the parameters and check to see if they are as expected.
        List<AnyContent> textInput = getInput(parameters, INPUT__TEXT_TO_CHECK);
        if (textInput.size() != 1) {
            throw new IllegalArgumentException(String.format("This service expects one input to be provided named '%s'", INPUT__TEXT_TO_CHECK));
        }
        List<AnyContent> expectedInput = getInput(parameters, INPUT__EXPECTED_TEXT);
        if (expectedInput.size() != 1) {
            throw new IllegalArgumentException(String.format("This service expects one input to be provided named '%s'", INPUT__EXPECTED_TEXT));
        }
        List<AnyContent> mismatchIsErrorInput = getInput(parameters, INPUT__MISMATCH_IS_ERROR);
        boolean mismatchIsError = true;
        if (!mismatchIsErrorInput.isEmpty()) {
            if (mismatchIsErrorInput.size() > 1) {
                throw new IllegalArgumentException(String.format("This service expects at most one input to be provided named '%s'", INPUT__MISMATCH_IS_ERROR));
            } else {
                mismatchIsError = Boolean.valueOf(mismatchIsErrorInput.get(0).getValue());
            }
        }
        // Run the validation and extract the report.
        result.setReport(doValidation(textInput.get(0).getValue(), expectedInput.get(0).getValue(), mismatchIsError));
        return result;
    }

    /**
     * Lookup a provided input from the received request parameters.
     *
     * @param parameters The request's parameters.
     * @param inputName The name of the input to lookup.
     * @return The inputs found to match the parameter name (not null).
     */
    private List<AnyContent> getInput(ValidateRequest parameters, String inputName) {
        List<AnyContent> inputs = new ArrayList<>();
        if (parameters != null) {
            if (parameters.getInput() != null) {
                for (AnyContent anInput: parameters.getInput()) {
                    if (inputName.equals(anInput.getName())) {
                        inputs.add(anInput);
                    }
                }
            }
        }
        return inputs;
    }

    /**
     * Carry out the validation and construct the validation report.
     *
     * For complex validators a good practice is to treat this class solely as a GITB-specific facade over the
     * validation. The validation itself should be decoupled into a separate class that is agnostic of the actual API
     * through which it is exposed. In this implementation, although overkill, this is illustrated by use of the
     * Validator class.
     *
     * @param providedText The text to validate.
     * @param expectedText The expected value.
     * @param mismatchIsError Whether or not a mismatch should be an error.
     * @return
     */
    private TAR doValidation(String providedText, String expectedText, boolean mismatchIsError) {
        TAR report = new TAR();

        // Add the current timestamp to the report.
        report.setReports(new TestAssertionGroupReportsType());
        try {
            GregorianCalendar calendar = new GregorianCalendar();
            report.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }

        // Add the input received in the report's context to be reported back to the client.
        report.setContext(new AnyContent());
        report.getContext().getItem().add(createAnyContentSimple(INPUT__TEXT_TO_CHECK, providedText, ValueEmbeddingEnumeration.STRING));
        report.getContext().getItem().add(createAnyContentSimple(INPUT__EXPECTED_TEXT, expectedText, ValueEmbeddingEnumeration.STRING));

        // Run the validation.
        ValidationResult result = validator.validate(providedText, expectedText);

        // Construct the report's items based on the validation result.
        int infos = 0;
        int warnings = 0;
        int errors = 0;
        report.setReports(new TestAssertionGroupReportsType());
        if (!result.isTextsMatch()) {
            if (mismatchIsError) {
                errors += 1;
                addReportItemError("The texts do not match.", report.getReports().getInfoOrWarningOrError());
            } else {
                warnings += 1;
                addReportItemWarning("The texts do not match.", report.getReports().getInfoOrWarningOrError());
            }
            if (result.isTextsMatchCaseInsensitive()) {
                // In this case report an information message.
                infos += 1;
                addReportItemInfo("The texts match but only when ignoring case.", report.getReports().getInfoOrWarningOrError());
            }
        }

        // Add the overall validation counters to the report.
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfAssertions(BigInteger.valueOf(infos));
        report.getCounters().setNrOfWarnings(BigInteger.valueOf(warnings));
        report.getCounters().setNrOfErrors(BigInteger.valueOf(errors));

        // Determine the overall result to report based on the validation results.
        if (errors == 0) {
            report.setResult(TestResultType.SUCCESS);
        } else {
            report.setResult(TestResultType.FAILURE);
        }
        LOG.info("Completed validation with result {}. Resulted in {} error(s), {} warning(s) and {} information message(s).", report.getResult(), errors, warnings, infos);
        return report;
    }

    /**
     * Add an information message to the report.
     *
     * @param message The message.
     * @param reportItems The report's items.
     */
    private void addReportItemInfo(String message, List<JAXBElement<TestAssertionReportType>> reportItems) {
        reportItems.add(objectFactory.createTestAssertionGroupReportsTypeInfo(createReportItemContent(message)));
    }

    /**
     * Add a warning message to the report.
     *
     * @param message The message.
     * @param reportItems The report's items.
     */
    private void addReportItemWarning(String message, List<JAXBElement<TestAssertionReportType>> reportItems) {
        reportItems.add(objectFactory.createTestAssertionGroupReportsTypeWarning(createReportItemContent(message)));
    }

    /**
     * Add an error message to the report.
     *
     * @param message The message.
     * @param reportItems The report's items.
     */
    private void addReportItemError(String message, List<JAXBElement<TestAssertionReportType>> reportItems) {
        reportItems.add(objectFactory.createTestAssertionGroupReportsTypeError(createReportItemContent(message)));
    }

    /**
     * Create the internal content of a report's item.
     *
     * @param message The message.
     * @return The content to wrap.
     */
    private BAR createReportItemContent(String message) {
        BAR itemContent = new BAR();
        itemContent.setDescription(message);
        return itemContent;
    }

    /**
     * Create a AnyContent object value based on the provided parameters.
     *
     * @param name The name of the value.
     * @param value The value itself.
     * @param embeddingMethod The way in which this value is to be considered.
     * @return The value.
     */
    private AnyContent createAnyContentSimple(String name, String value, ValueEmbeddingEnumeration embeddingMethod) {
        AnyContent input = new AnyContent();
        input.setName(name);
        input.setValue(value);
        input.setType("string");
        input.setEmbeddingMethod(embeddingMethod);
        return input;
    }

}
