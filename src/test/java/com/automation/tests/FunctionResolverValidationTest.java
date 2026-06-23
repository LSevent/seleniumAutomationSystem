package com.automation.tests;

import com.automation.context.StepContextHolder;
import com.automation.engine.FunctionResolver;
import com.automation.models.KeywordExecutionResult;
import com.automation.models.KeywordSourceType;
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
    public void blankKeywordShouldFailClearly() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve("BRS", " ")
        );

        Assert.assertEquals(exception.getMessage(), "Keyword name is required.");
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

        KeywordExecutionResult result = execute(resolver, "UNKNOWN", "verifyText", MESSAGE_XPATH, "Ready");

        Assert.assertEquals(result.getSourceType(), KeywordSourceType.BASE);
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

    private KeywordExecutionResult execute(
            FunctionResolver resolver,
            String application,
            String keyword,
            String resolvedXpath,
            String resolvedValue
    ) {
        StepContextHolder.set(ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioAction("Resolver Validation")
                .scenarioName("Resolver Validation")
                .sheetName("Resolver Validation")
                .testcaseName("Validation keywords")
                .testcaseParentRow(2)
                .excelRow(3)
                .stepNumber(1)
                .keyword(keyword)
                .objectName(resolvedXpath == null || resolvedXpath.isBlank() ? "" : "testObject")
                .application(application)
                .description("Resolver validation step")
                .rawValue(resolvedValue)
                .resolvedValue(resolvedValue)
                .rawXPath(resolvedXpath)
                .resolvedXPath(resolvedXpath)
                .executedBy("")
                .build());
        try {
            return resolver.execute(application, keyword);
        } finally {
            StepContextHolder.clear();
        }
    }
}
