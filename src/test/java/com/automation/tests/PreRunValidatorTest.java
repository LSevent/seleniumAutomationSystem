package com.automation.tests;

import com.automation.exceptions.FrameworkException;
import com.automation.models.ResolvedScenarioContext;
import com.automation.models.ResolvedStepContext;
import com.automation.models.ResolvedTestcaseContext;
import com.automation.validation.PreRunValidator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class PreRunValidatorTest {

    private final PreRunValidator validator = new PreRunValidator();

    @Test
    public void validPlanShouldPass() {
        validator.validate(plan(
                step("openUrl", "", "CONFIG.BASE_URL", "file:///test.html", "", "", "BRS"),
                step("input", "txtUsername", "LOGIN_DATA.USERNAME", "brs_admin", "//input[@id='username']", "//input[@id='username']", "BRS"),
                step("click", "btnLogin", "", "", "//button[@id='login']", "//button[@id='login']", "BRS"),
                step("ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE", "Single Meeting = Single Meeting", "", "", "BRS", "Single Meeting"),
                step("else", "", "", "", "", "", "BRS"),
                step("endIf", "", "", "", "", "", "BRS"),
                step("forEachDataRow", "", "BOOKING_DATA", "BOOKING_DATA row 1 of 2", "", "", "BRS"),
                step("endForEachDataRow", "", "", "BOOKING_DATA row 1 of 2", "", "", "BRS")
        ));
    }

    @Test
    public void keywordRuntimeRequirementsShouldNotBlockPreRunValidation() {
        validator.validate(plan(
                step("openUrl", "", "", "", "", "", "BRS"),
                step("input", "txtUsername", "", "", "", "", "BRS"),
                step("click", "btnMissing", "", "", "", "", "BRS"),
                step("approveBooking", "", "", "", "", "", "BRS"),
                step("screenshotPartByObject", "", "", "", "", "", "BRS")
        ));
    }

    @Test
    public void blankKeywordShouldFailClearly() {
        FrameworkException exception = validationFailure(step("", "", "", "", "", "", "BRS"));

        assertContainsContext(exception, "Keyword is required for active step.", "", "", "BRS");
    }

    @Test
    public void blankApplicationShouldFailClearly() {
        FrameworkException exception = validationFailure(step("click", "btnLogin", "", "", "", "", ""));

        assertContainsContext(exception, "Application is required for active step.", "click", "btnLogin", "");
    }

    @Test
    public void conditionalDirectivesShouldSupportDescriptionAsExpectedValue() {
        validator.validate(plan(
                step("ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE", "Single Meeting = Single Meeting", "", "", "BRS", "Single Meeting"),
                step("elseIfEquals", "", "=BOOKING_DATA!$B$1", "Repeating Meeting = Repeating Meeting", "", "", "BRS", "Repeating Meeting"),
                step("else", "", "", "", "", "", "BRS"),
                step("endIf", "", "", "", "", "", "BRS")
        ));
    }

    @Test
    public void conditionalComparisonDirectivesShouldRequireValueCondition() {
        FrameworkException exception = validationFailure(step("ifEquals", "", "", "", "", "", "BRS"));

        assertContainsContext(exception, "Value condition is required for keyword 'ifEquals'.", "ifEquals", "", "BRS");
    }

    @Test
    public void conditionalComparisonDirectivesShouldRequireDescriptionWhenValueContainsActualOnly() {
        FrameworkException exception = validationFailure(step(
                "ifEquals",
                "",
                "=BOOKING_DATA!$B$1",
                "Repeating Meeting",
                "",
                "",
                "BRS",
                ""
        ));

        assertContainsContext(
                exception,
                "Expected value is required in Description for keyword 'ifEquals' when Value contains only the actual value.",
                "ifEquals",
                "",
                "BRS"
        );
    }

    @Test
    public void conditionalComparisonDirectivesShouldRequireValidInlineConditionExpression() {
        FrameworkException exception = validationFailure(step(
                "ifEquals",
                "",
                "BOOKING_DATA.SCHEDULE_TYPE =",
                "Single Meeting =",
                "",
                "",
                "BRS",
                "Single meeting condition"
        ));

        assertContainsContext(
                exception,
                "Invalid condition expression: BOOKING_DATA.SCHEDULE_TYPE =. Expected format: ACTUAL = EXPECTED.",
                "ifEquals",
                "",
                "BRS"
        );
    }

    @Test
    public void forEachDataRowShouldRequireDataSheetName() {
        FrameworkException exception = validationFailure(step("forEachDataRow", "", "", "", "", "", "BRS"));

        assertContainsContext(exception, "Data sheet name is required for keyword 'forEachDataRow'.", "forEachDataRow", "", "BRS");
    }

    @Test
    public void activeScenarioWithoutTestcaseShouldFailClearly() {
        FrameworkException exception = Assert.expectThrows(
                FrameworkException.class,
                () -> validator.validate(List.of(new ResolvedScenarioContext("1", "Booking Flow", "Booking", List.of())))
        );

        Assert.assertTrue(exception.getMessage().contains("Active scenario has no active testcase."));
        Assert.assertTrue(exception.getMessage().contains("Scenario NO: 1."));
    }

    @Test
    public void activeTestcaseWithoutStepShouldFailClearly() {
        ResolvedTestcaseContext testcase = new ResolvedTestcaseContext("Empty Testcase", "BRS", 2, List.of());
        FrameworkException exception = Assert.expectThrows(
                FrameworkException.class,
                () -> validator.validate(List.of(new ResolvedScenarioContext("1", "Booking Flow", "Booking", List.of(testcase))))
        );

        Assert.assertTrue(exception.getMessage().contains("Active testcase has no steps."));
        Assert.assertTrue(exception.getMessage().contains("Testcase: Empty Testcase."));
        Assert.assertTrue(exception.getMessage().contains("Row: 2."));
    }

    @Test
    public void multipleStructuralErrorsShouldBeCollected() {
        FrameworkException exception = validationFailure(step("", "", "", "", "", "", ""));

        Assert.assertTrue(exception.getMessage().startsWith("Pre-run validation failed with 2 error(s)."));
        Assert.assertTrue(exception.getMessage().contains("1. Keyword is required for active step."));
        Assert.assertTrue(exception.getMessage().contains("2. Application is required for active step."));
    }

    private FrameworkException validationFailure(ResolvedStepContext step) {
        return Assert.expectThrows(FrameworkException.class, () -> validator.validate(plan(step)));
    }

    private List<ResolvedScenarioContext> plan(ResolvedStepContext... steps) {
        ResolvedTestcaseContext testcase = new ResolvedTestcaseContext(
                "Login BRS",
                "BRS",
                2,
                List.of(steps)
        );
        return List.of(new ResolvedScenarioContext("1", "Local Keyword Test", "Local test", List.of(testcase)));
    }

    private ResolvedStepContext step(
            String keyword,
            String object,
            String rawValue,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath,
            String application
    ) {
        return step(keyword, object, rawValue, resolvedValue, rawXPath, resolvedXPath, application, "Validation step");
    }

    private ResolvedStepContext step(
            String keyword,
            String object,
            String rawValue,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath,
            String application,
            String description
    ) {
        return ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioAction("Local Keyword Test")
                .scenarioName("Local test")
                .sheetName("Local Keyword Test")
                .testcaseName("Login BRS")
                .testcaseParentRow(2)
                .excelRow(3)
                .stepNumber(1)
                .keyword(keyword)
                .objectName(object)
                .application(application)
                .description(description)
                .rawValue(rawValue)
                .resolvedValue(resolvedValue)
                .rawXPath(rawXPath)
                .resolvedXPath(resolvedXPath)
                .executedBy("")
                .build();
    }

    private void assertContainsContext(
            FrameworkException exception,
            String expectedError,
            String keyword,
            String object,
            String application
    ) {
        String message = exception.getMessage();
        Assert.assertTrue(message.contains(expectedError));
        Assert.assertTrue(message.contains("Scenario NO: 1."));
        Assert.assertTrue(message.contains("Scenario ACTION: Local Keyword Test."));
        Assert.assertTrue(message.contains("Sheet: Local Keyword Test."));
        Assert.assertTrue(message.contains("Testcase: Login BRS."));
        Assert.assertTrue(message.contains("Row: 3."));
        if (!keyword.isBlank()) {
            Assert.assertTrue(message.contains("Keyword: " + keyword + "."));
        }
        if (!object.isBlank()) {
            Assert.assertTrue(message.contains("Object: " + object + "."));
        }
        if (!application.isBlank()) {
            Assert.assertTrue(message.contains("Application: " + application + "."));
        }
    }
}
