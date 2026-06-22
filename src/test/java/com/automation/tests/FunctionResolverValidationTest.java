package com.automation.tests;

import com.automation.context.StepContextHolder;
import com.automation.engine.FunctionResolver;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.FunctionSourceType;
import com.automation.models.ResolvedStepContext;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class FunctionResolverValidationTest {

    private static final String BUTTON_XPATH = "//button[@id='login']";
    private static final String MESSAGE_XPATH = "//div[@id='message']";

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        StepContextHolder.clear();
    }

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
                () -> execute(resolver, "BRS", "approveBooking", BUTTON_XPATH, "")
        );

        Assert.assertTrue(exception.getMessage().contains(
                "Keyword 'approveBooking' not found in SpecificFunction for application 'BRS' or BaseFunction."
        ));
    }

    @Test
    public void unknownApplicationShouldFallbackToBaseFunctionWhenKeywordExists() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        FunctionExecutionResult result = execute(resolver, "UNKNOWN", "verifyText", MESSAGE_XPATH, "Ready");

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.BASE);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.base.BaseFunction");
    }

    @Test
    public void unknownApplicationAndUnknownKeywordShouldFailClearly() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> execute(resolver, "UNKNOWN", "approveBooking", BUTTON_XPATH, "")
        );

        Assert.assertTrue(exception.getMessage().contains(
                "Keyword 'approveBooking' not found in SpecificFunction for application 'UNKNOWN' or BaseFunction."
        ));
    }

    @Test
    public void executionFailureShouldNameKeywordAndFunctionClass() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        AssertionError error = Assert.expectThrows(
                AssertionError.class,
                () -> execute(resolver, "UNKNOWN", "click", "//button[@id='missing']", "")
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

    private FunctionExecutionResult execute(
            FunctionResolver resolver,
            String application,
            String function,
            String resolvedXpath,
            String resolvedValue
    ) {
        StepContextHolder.set(new ResolvedStepContext(
                "1",
                "Resolver Validation",
                "Resolver Validation",
                "Resolver Validation",
                "Validation keywords",
                2,
                3,
                1,
                function,
                resolvedXpath == null || resolvedXpath.isBlank() ? "" : "testObject",
                application,
                "Resolver validation step",
                resolvedValue,
                resolvedValue,
                resolvedXpath,
                resolvedXpath,
                ""
        ));
        try {
            return resolver.execute(application, function);
        } finally {
            StepContextHolder.clear();
        }
    }
}
