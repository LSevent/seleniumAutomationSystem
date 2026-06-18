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
                step("verifyText", "message", "DATA.MESSAGE", "Success", "//div[@id='message']", "//div[@id='message']", "BRS"),
                step("verifyDisplayed", "dashboard", "", "", "//h1", "//h1", "BRS"),
                step("screenshot", "", "", "", "", "", "BRS")
        ));
    }

    @Test
    public void missingObjectShouldFailClearly() {
        FrameworkException exception = validationFailure(step("click", "", "", "", "", "", "BRS"));

        assertContainsContext(exception, "Object is required for keyword 'click'.", "click", "", "BRS");
    }

    @Test
    public void missingXpathShouldFailClearly() {
        FrameworkException exception = validationFailure(step("click", "btnMissing", "", "", "", "", "BRS"));

        Assert.assertTrue(exception.getMessage().contains("Object was not resolved from OBJECT_REPOSITORY."));
        assertContainsContext(exception, "XPath is required for keyword 'click'.", "click", "btnMissing", "BRS");
    }

    @Test
    public void blankFunctionShouldFailClearly() {
        FrameworkException exception = validationFailure(step("", "", "", "", "", "", "BRS"));

        assertContainsContext(exception, "Function is required for active step.", "", "", "BRS");
    }

    @Test
    public void openUrlMissingValueShouldFailClearly() {
        FrameworkException exception = validationFailure(step("openUrl", "", "", "", "", "", "BRS"));

        assertContainsContext(exception, "Value is required for keyword 'openUrl'.", "openUrl", "", "BRS");
    }

    @Test
    public void inputMissingValueShouldFailClearly() {
        FrameworkException exception = validationFailure(step(
                "input", "txtUsername", "", "", "//input[@id='username']", "//input[@id='username']", "BRS"
        ));

        assertContainsContext(exception, "Value is required for keyword 'input'.", "input", "txtUsername", "BRS");
    }

    @Test
    public void clickMissingXpathShouldFailClearly() {
        FrameworkException exception = validationFailure(step("click", "btnLogin", "", "", "", "", "BRS"));

        assertContainsContext(exception, "XPath is required for keyword 'click'.", "click", "btnLogin", "BRS");
    }

    @Test
    public void screenshotWithoutObjectShouldPass() {
        validator.validate(plan(step("screenshot", "", "", "", "", "", "BRS")));
    }

    @Test
    public void inactiveScenarioAndTestcaseOutsideResolvedPlanShouldNotBlockValidation() {
        validator.validate(plan(step("click", "btnLogin", "", "", "//button", "//button", "BRS")));
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
    public void invalidDataReferenceShouldFailClearly() {
        FrameworkException exception = validationFailure(step(
                "input", "txtUsername", "LOGIN_DATA.USER.NAME", "", "//input", "//input", "BRS"
        ));

        Assert.assertTrue(exception.getMessage().contains(
                "Invalid data reference format: LOGIN_DATA.USER.NAME. Expected format: SHEET_NAME.COLUMN_NAME."
        ));
    }

    @Test
    public void unresolvedDynamicXpathShouldFailClearly() {
        FrameworkException exception = validationFailure(step(
                "click",
                "btnRoomByName",
                "BOOKING_DATA.ROOM_NAME",
                "Meeting Room A",
                "//button[contains(text(),'{ROOM_NAME}')]",
                "//button[contains(text(),'{ROOM_NAME}')]",
                "BRS"
        ));

        Assert.assertTrue(exception.getMessage().contains("XPath placeholder {ROOM_NAME} was not resolved."));
    }

    @Test
    public void multipleErrorsShouldBeCollected() {
        FrameworkException exception = validationFailure(step("", "", "", "", "", "", ""));

        Assert.assertTrue(exception.getMessage().startsWith("Pre-run validation failed with 2 error(s)."));
        Assert.assertTrue(exception.getMessage().contains("1. Function is required for active step."));
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
            String function,
            String object,
            String rawValue,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath,
            String application
    ) {
        return new ResolvedStepContext(
                "1",
                "Local Keyword Test",
                "Local test",
                "Local Keyword Test",
                "Login BRS",
                2,
                3,
                1,
                function,
                object,
                application,
                "Validation step",
                rawValue,
                resolvedValue,
                rawXPath,
                resolvedXPath,
                ""
        );
    }

    private void assertContainsContext(
            FrameworkException exception,
            String expectedError,
            String function,
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
        if (!function.isBlank()) {
            Assert.assertTrue(message.contains("Function: " + function + "."));
        }
        if (!object.isBlank()) {
            Assert.assertTrue(message.contains("Object: " + object + "."));
        }
        if (!application.isBlank()) {
            Assert.assertTrue(message.contains("Application: " + application + "."));
        }
    }
}
