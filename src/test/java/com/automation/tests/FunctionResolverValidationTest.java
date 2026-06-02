package com.automation.tests;

import com.automation.engine.FunctionResolver;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.FunctionSourceType;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FunctionResolverValidationTest {

    private static final String BUTTON_XPATH = "//button[@id='login']";
    private static final String MESSAGE_XPATH = "//div[@id='message']";

    @Test
    public void blankFunctionShouldFailClearly() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve("BRS", " ")
        );

        Assert.assertEquals(exception.getMessage(), "Function name is required.");
    }

    @Test
    public void unknownKeywordShouldFailClearly() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> resolver.execute("BRS", "approveBooking", BUTTON_XPATH, "")
        );

        Assert.assertEquals(exception.getMessage(), "Keyword 'approveBooking' not found in SpecificFunction for application 'BRS' or BaseFunction.");
    }

    @Test
    public void missingXpathShouldFailClearly() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> resolver.execute("BRS", "click", "", "")
        );

        Assert.assertEquals(exception.getMessage(), "XPath is required for keyword 'click'.");
    }

    @Test
    public void missingInputValueShouldFailClearly() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> resolver.execute("BRS", "input", "//input[@id='username']", "")
        );

        Assert.assertEquals(exception.getMessage(), "Value is required for keyword 'input'.");
    }

    @Test
    public void missingOpenUrlValueShouldFailClearly() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> resolver.execute("BRS", "openUrl", "", "")
        );

        Assert.assertEquals(exception.getMessage(), "URL is required for keyword 'openUrl'.");
    }

    @Test
    public void missingVerifyTextValueShouldFailClearly() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> resolver.execute("BRS", "verifyText", MESSAGE_XPATH, "")
        );

        Assert.assertEquals(exception.getMessage(), "Expected text is required for keyword 'verifyText'.");
    }

    @Test
    public void unknownApplicationShouldFallbackToBaseFunctionWhenKeywordExists() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        FunctionExecutionResult result = resolver.execute("UNKNOWN", "verifyText", MESSAGE_XPATH, "Ready");

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.BASE);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.base.BaseFunction");
    }

    @Test
    public void unknownApplicationAndUnknownKeywordShouldFailClearly() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> resolver.execute("UNKNOWN", "approveBooking", BUTTON_XPATH, "")
        );

        Assert.assertEquals(exception.getMessage(), "Keyword 'approveBooking' not found in SpecificFunction for application 'UNKNOWN' or BaseFunction.");
    }

    @Test
    public void executionFailureShouldNameKeywordAndFunctionClass() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        AssertionError error = Assert.expectThrows(
                AssertionError.class,
                () -> resolver.execute("UNKNOWN", "click", "//button[@id='missing']", "")
        );

        Assert.assertTrue(error.getMessage().contains("Failed to execute keyword 'click' using BaseFunction. Cause:"));
    }

    private FakeWebDriver driver() {
        FakeWebDriver fakeWebDriver = new FakeWebDriver();
        fakeWebDriver.setTitle("Validation");
        fakeWebDriver.addElement(BUTTON_XPATH, "Login");
        fakeWebDriver.addElement(MESSAGE_XPATH, "Ready");
        fakeWebDriver.addElement("//input[@id='username']", "");
        return fakeWebDriver;
    }
}
