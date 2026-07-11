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
                step("select", "sltScheduleType", "BOOKING_DATA.SCHEDULE_TYPE", "Repeating Meeting", "//select[@id='scheduleType']", "//select[@id='scheduleType']", "BRS"),
                step("click", "btnLogin", "", "", "//button[@id='login']", "//button[@id='login']", "BRS"),
                step("verifyText", "message", "DATA.MESSAGE", "Success", "//div[@id='message']", "//div[@id='message']", "BRS"),
                step("verifyDisplayed", "dashboard", "", "", "//h1", "//h1", "BRS"),
                step("verifyNotDisplayed", "loading", "", "", "//div[@id='loading']", "//div[@id='loading']", "BRS"),
                step("ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "", "", "BRS"),
                step("else", "", "", "", "", "", "BRS"),
                step("endIf", "", "", "", "", "", "BRS"),
                step("forEachDataRow", "", "BOOKING_DATA", "BOOKING_DATA row 1 of 2", "", "", "BRS"),
                step("endForEachDataRow", "", "", "BOOKING_DATA row 1 of 2", "", "", "BRS"),
                step("screenshot", "", "", "", "", "", "BRS"),
                step("screenshotFullPart", "", "", "", "", "", "BRS"),
                step("screenshotPartByObject", "pnlBooking", "Booking panel", "Booking panel", "//section[@id='booking']", "//section[@id='booking']", "BRS")
        ));
    }

    @Test
    public void allXPathDependentKeywordsShouldBeValidatedBeforeRuntime() {
        List<String> keywords = List.of(
                "click",
                "verifyDisplayed",
                "verifyNotDisplayed",
                "clear",
                "waitVisible",
                "waitClickable",
                "scrollToElement",
                "safeClick",
                "pressEnter",
                "screenshotPartByObject",
                "input",
                "select",
                "verifyText",
                "verifyTextContains",
                "selectRoomByName",
                "verifyBookingCreated",
                "verifyEmployeeVisible"
        );

        for (String keyword : keywords) {
            FrameworkException exception = validationFailure(step(
                    keyword, "targetObject", "literal value", "resolved value", "", "", "BRS"
            ));

            assertContainsContext(
                    exception,
                    "XPath is required for keyword '" + keyword + "'.",
                    keyword,
                    "targetObject",
                    "BRS"
            );
        }
    }

    @Test
    public void allValueDependentKeywordsShouldBeValidatedBeforeRuntime() {
        List<String> keywords = List.of(
                "openUrl",
                "verifyUrlContains",
                "verifyTitle",
                "verifyTitleContains",
                "input",
                "select",
                "verifyText",
                "verifyTextContains",
                "verifyBookingCreated",
                "verifyEmployeeVisible"
        );

        for (String keyword : keywords) {
            boolean xpathRequired = !keyword.equals("openUrl")
                    && !keyword.equals("verifyUrlContains")
                    && !keyword.equals("verifyTitle")
                    && !keyword.equals("verifyTitleContains");
            String objectName = xpathRequired ? "targetObject" : "";
            String resolvedXPath = xpathRequired ? "//div[@id='target']" : "";
            FrameworkException exception = validationFailure(step(
                    keyword, objectName, "", "", resolvedXPath, resolvedXPath, "BRS"
            ));

            assertContainsContext(
                    exception,
                    "Value is required for keyword '" + keyword + "'.",
                    keyword,
                    objectName,
                    "BRS"
            );
        }
    }

    @Test
    public void missingObjectShouldFailClearly() {
        FrameworkException exception = validationFailure(step("click", "", "", "", "", "", "BRS"));

        assertContainsContext(exception, "Object is required for keyword 'click'.", "click", "", "BRS");
    }

    @Test
    public void missingXPathShouldFailClearly() {
        FrameworkException exception = validationFailure(step("click", "btnMissing", "", "", "", "", "BRS"));

        Assert.assertTrue(exception.getMessage().contains("Object was not resolved from OBJECT_REPOSITORY."));
        assertContainsContext(exception, "XPath is required for keyword 'click'.", "click", "btnMissing", "BRS");
    }

    @Test
    public void blankKeywordShouldFailClearly() {
        FrameworkException exception = validationFailure(step("", "", "", "", "", "", "BRS"));

        assertContainsContext(exception, "Keyword is required for active step.", "", "", "BRS");
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
    public void clickMissingXPathShouldFailClearly() {
        FrameworkException exception = validationFailure(step("click", "btnLogin", "", "", "", "", "BRS"));

        assertContainsContext(exception, "XPath is required for keyword 'click'.", "click", "btnLogin", "BRS");
    }

    @Test
    public void screenshotWithoutObjectShouldPass() {
        validator.validate(plan(step("screenshot", "", "", "", "", "", "BRS")));
    }

    @Test
    public void screenshotFullPartWithoutObjectShouldPass() {
        validator.validate(plan(step("screenshotFullPart", "", "", "", "", "", "BRS")));
    }

    @Test
    public void screenshotPartByObjectShouldRequireObjectAndXPath() {
        FrameworkException missingObject = validationFailure(step("screenshotPartByObject", "", "", "", "", "", "BRS"));
        assertContainsContext(
                missingObject,
                "Object is required for keyword 'screenshotPartByObject'.",
                "screenshotPartByObject",
                "",
                "BRS"
        );

        FrameworkException missingXPath = validationFailure(step(
                "screenshotPartByObject",
                "pnlBooking",
                "",
                "",
                "",
                "",
                "BRS"
        ));
        assertContainsContext(
                missingXPath,
                "XPath is required for keyword 'screenshotPartByObject'.",
                "screenshotPartByObject",
                "pnlBooking",
                "BRS"
        );
    }

    @Test
    public void unknownKeywordShouldFailBeforeRuntime() {
        FrameworkException exception = validationFailure(step("approveBooking", "", "", "", "", "", "BRS"));

        assertContainsContext(
                exception,
                "Unknown keyword 'approveBooking' for application 'BRS'.",
                "approveBooking",
                "",
                "BRS"
        );
        Assert.assertTrue(exception.getMessage().contains(
                "Add a public no-argument method named 'approveBooking' to SpecificFunction for application 'BRS' or BaseFunction."
        ));
    }

    @Test
    public void applicationSpecificKeywordShouldPassKnownKeywordValidation() {
        validator.validate(plan(step(
                "selectRoomByName",
                "btnRoomByName",
                "BOOKING_DATA.ROOM_NAME",
                "Meeting Room A",
                "//button[contains(text(),'{ROOM_NAME}')]",
                "//button[contains(text(),'Meeting Room A')]",
                "BRS"
        )));
    }

    @Test
    public void selectRoomByNameShouldNotRequireValueForStaticXPath() {
        validator.validate(plan(step(
                "selectRoomByName",
                "btnDefaultRoom",
                "",
                "",
                "//button[@id='default-room']",
                "//button[@id='default-room']",
                "BRS"
        )));
    }

    @Test
    public void conditionalDirectivesShouldNotRequireObjectOrXPath() {
        validator.validate(plan(
                step("ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "", "", "BRS"),
                step("elseIfEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Repeating Meeting", "BOOKING_DATA.SCHEDULE_TYPE = Repeating Meeting", "", "", "BRS"),
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
    public void conditionalComparisonDirectivesShouldRequireValidConditionExpression() {
        FrameworkException exception = validationFailure(step(
                "ifEquals",
                "",
                "BOOKING_DATA.SCHEDULE_TYPE",
                "Single Meeting",
                "",
                "",
                "BRS"
        ));

        assertContainsContext(
                exception,
                "Invalid condition expression: BOOKING_DATA.SCHEDULE_TYPE. Expected format: ACTUAL = EXPECTED.",
                "ifEquals",
                "",
                "BRS"
        );
    }

    @Test
    public void loopDirectivesShouldNotRequireObjectOrXPath() {
        validator.validate(plan(
                step("forEachDataRow", "", "BOOKING_DATA", "BOOKING_DATA row 1 of 2", "", "", "BRS"),
                step("endForEachDataRow", "", "", "BOOKING_DATA row 1 of 2", "", "", "BRS")
        ));
    }

    @Test
    public void forEachDataRowShouldRequireDataSheetName() {
        FrameworkException exception = validationFailure(step("forEachDataRow", "", "", "", "", "", "BRS"));

        assertContainsContext(exception, "Data sheet name is required for keyword 'forEachDataRow'.", "forEachDataRow", "", "BRS");
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
    public void multiDotLiteralShouldPassValidationWhenAlreadyResolved() {
        validator.validate(plan(step(
                "input",
                "txtUsername",
                "john.middle.doe",
                "john.middle.doe",
                "//input",
                "//input",
                "BRS"
        )));
    }

    @Test
    public void unresolvedDynamicXPathShouldFailClearly() {
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
                .description("Validation step")
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
