package com.automation.tests;

import com.automation.base.BaseFunction;
import com.automation.context.EvidenceContextHolder;
import com.automation.context.ScreenshotContextHolder;
import com.automation.context.StepContextHolder;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ResolvedStepContext;
import com.automation.reports.ExcelReportConfig;
import com.automation.services.ScreenshotService;
import com.automation.tests.support.FakeWebDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;

@Test(singleThreaded = true)
public class BaseFunctionContextTest {

    @AfterMethod(alwaysRun = true)
    public void clearContext() {
        ScreenshotContextHolder.clear();
        EvidenceContextHolder.clear();
        StepContextHolder.clear();
    }

    @Test
    public void openUrlShouldUseResolvedValueFromCurrentContext() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step("openUrl", "", "CONFIG.BASE_URL", "file:///resolved.html", "", ""));

        new BaseFunction(driver.driver()).openUrl();

        Assert.assertEquals(driver.getCurrentUrl(), "file:///resolved.html");
    }

    @Test
    public void clickShouldUseResolvedXPathInsteadOfObjectNameOrRawXPath() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step(
                "click",
                "not-an-xpath",
                "",
                "",
                "//button[@id='raw-login']",
                "//button[@id='resolved-login']"
        ));

        new BaseFunction(driver.driver()).click();

        Assert.assertTrue(driver.element("//button[@id='resolved-login']").isClicked());
    }

    @Test
    public void inputShouldUseResolvedXPathAndResolvedValue() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step(
                "input",
                "txtUsername",
                "LOGIN_DATA.USERNAME",
                "resolved_user",
                "//input[@id='raw-username']",
                "//input[@id='resolved-username']"
        ));

        new BaseFunction(driver.driver()).input();

        Assert.assertEquals(driver.element("//input[@id='resolved-username']").getValue(), "resolved_user");
    }

    @Test
    public void selectShouldUseResolvedXPathAndResolvedValue() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step(
                "select",
                "sltScheduleType",
                "BOOKING_DATA.SCHEDULE_TYPE",
                "Repeating Meeting",
                "//select[@id='raw-schedule-type']",
                "//select[@id='resolved-schedule-type']"
        ));

        new BaseFunction(driver.driver()).select();

        Assert.assertEquals(
                driver.element("//select[@id='resolved-schedule-type']").getSelectedOption(),
                "Repeating Meeting"
        );
    }

    @Test
    public void verifyDisplayedShouldUseResolvedXPath() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step(
                "verifyDisplayed",
                "lblDashboard",
                "",
                "",
                "//h1[@id='raw-dashboard']",
                "//h1[@id='resolved-dashboard']"
        ));

        new BaseFunction(driver.driver()).verifyDisplayed();
    }

    @Test
    public void verifyNotDisplayedShouldAcceptHiddenResolvedElement() {
        FakeWebDriver driver = driver();
        driver.element("//div[@id='resolved-message']").setDisplayed(false);
        StepContextHolder.set(step(
                "verifyNotDisplayed",
                "lblMessage",
                "",
                "",
                "//div[@id='raw-message']",
                "//div[@id='resolved-message']"
        ));

        new BaseFunction(driver.driver()).verifyNotDisplayed();
    }

    @Test
    public void verifyTextShouldUseResolvedXPathAndResolvedValue() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step(
                "verifyText",
                "lblMessage",
                "LOGIN_DATA.MESSAGE",
                "Ready",
                "//div[@id='raw-message']",
                "//div[@id='resolved-message']"
        ));

        new BaseFunction(driver.driver()).verifyText();
    }

    @Test
    public void screenshotShouldUseDescriptionLabelAndRegisterEvidence() {
        FakeWebDriver driver = driver();
        RecordingScreenshotService screenshotService = new RecordingScreenshotService();
        StepContextHolder.set(step(
                "screenshot",
                "",
                "Value should not label screenshot",
                "Value should not label screenshot",
                "",
                ""
        ));
        EvidenceContextHolder.start();
        ScreenshotContextHolder.set(screenshotService, new ExcelReportConfig(false, true, true));

        new BaseFunction(driver.driver()).screenshot();

        Assert.assertEquals(EvidenceContextHolder.getAll(), List.of("target/screenshots/manual.png"));
        Assert.assertTrue(screenshotService.observedScreenshotName.contains("BaseFunction context test"));
        Assert.assertFalse(screenshotService.observedScreenshotName.contains("Value should not label screenshot"));
    }

    @Test
    public void screenshotPartByObjectShouldUseDescriptionLabelAndRegisterMultipleEvidenceItems() {
        FakeWebDriver driver = driver();
        RecordingScreenshotService screenshotService = new RecordingScreenshotService();
        StepContextHolder.set(step(
                "screenshotPartByObject",
                "pnlBooking",
                "Value should not label object screenshot",
                "Value should not label object screenshot",
                "//div[@id='raw-message']",
                "//div[@id='resolved-message']"
        ));
        EvidenceContextHolder.start();
        ScreenshotContextHolder.set(screenshotService, new ExcelReportConfig(false, true, true));

        new BaseFunction(driver.driver()).screenshotPartByObject();

        Assert.assertEquals(
                EvidenceContextHolder.getAll(),
                List.of("target/screenshots/object-part-1.png", "target/screenshots/object-part-2.png")
        );
        Assert.assertTrue(screenshotService.observedScreenshotName.contains("BaseFunction context test"));
        Assert.assertFalse(screenshotService.observedScreenshotName.contains("Value should not label object screenshot"));
    }

    @Test
    public void screenshotFullPartShouldUseDescriptionLabelAndRegisterMultipleEvidenceItems() {
        FakeWebDriver driver = driver();
        RecordingScreenshotService screenshotService = new RecordingScreenshotService();
        StepContextHolder.set(step(
                "screenshotFullPart",
                "",
                "Value should not label full screenshot",
                "Value should not label full screenshot",
                "",
                ""
        ));
        EvidenceContextHolder.start();
        ScreenshotContextHolder.set(screenshotService, new ExcelReportConfig(false, true, true));

        new BaseFunction(driver.driver()).screenshotFullPart();

        Assert.assertEquals(
                EvidenceContextHolder.getAll(),
                List.of("target/screenshots/full-part-1.png", "target/screenshots/full-part-2.png")
        );
        Assert.assertTrue(screenshotService.observedScreenshotName.contains("BaseFunction context test"));
        Assert.assertFalse(screenshotService.observedScreenshotName.contains("Value should not label full screenshot"));
    }

    @Test
    public void sharedContextHelpersShouldReadFromStepContextHolder() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step(
                "customKeyword",
                "customObject",
                "RAW_VALUE",
                "resolved value",
                "//raw",
                "//resolved"
        ));
        ContextHelperProbe keyword = new ContextHelperProbe(driver.driver());

        Assert.assertSame(keyword.step(), StepContextHolder.get());
        Assert.assertEquals(keyword.resolvedXPath(), "//resolved");
        Assert.assertEquals(keyword.resolvedValue(), "resolved value");
        Assert.assertEquals(keyword.unresolvedValue(), "RAW_VALUE");
        Assert.assertEquals(keyword.stepObjectName(), "customObject");
        Assert.assertEquals(keyword.stepApplication(), "BRS");
    }

    @Test
    public void clickShouldFailClearlyWhenContextIsMissing() {
        FrameworkException exception = Assert.expectThrows(
                FrameworkException.class,
                () -> new BaseFunction(driver().driver()).click()
        );

        Assert.assertEquals(
                exception.getMessage(),
                "Step context is not available. Keyword must be executed through KeywordEngine."
        );
    }

    private FakeWebDriver driver() {
        FakeWebDriver driver = new FakeWebDriver();
        driver.addElement("//button[@id='resolved-login']", "Login");
        driver.addElement("//input[@id='resolved-username']", "");
        driver.addElement("//input[@id='resolved-password']", "");
        driver.addElement("//h1[@id='resolved-dashboard']", "Dashboard");
        driver.addElement("//div[@id='resolved-message']", "Ready");
        driver.addSelect(
                "//select[@id='resolved-schedule-type']",
                "Single Meeting",
                "Repeating Meeting"
        );
        return driver;
    }

    private ResolvedStepContext step(
            String keyword,
            String objectName,
            String rawValue,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath
    ) {
        return ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioAction("Local Keyword Test")
                .scenarioName("Context-based BaseFunction keywords")
                .sheetName("Local Keyword Test")
                .testcaseName("Login BRS")
                .testcaseParentRow(4)
                .excelRow(7)
                .stepNumber(1)
                .keyword(keyword)
                .objectName(objectName)
                .application("BRS")
                .description("BaseFunction context test")
                .rawValue(rawValue)
                .resolvedValue(resolvedValue)
                .rawXPath(rawXPath)
                .resolvedXPath(resolvedXPath)
                .executedBy("")
                .build();
    }

    private static final class ContextHelperProbe extends BaseFunction {

        private ContextHelperProbe(org.openqa.selenium.WebDriver driver) {
            super(driver);
        }

        private ResolvedStepContext step() {
            return currentStep();
        }

        private String resolvedXPath() {
            return xpath();
        }

        private String resolvedValue() {
            return value();
        }

        private String unresolvedValue() {
            return rawValue();
        }

        private String stepObjectName() {
            return objectName();
        }

        private String stepApplication() {
            return application();
        }
    }

    private static final class RecordingScreenshotService extends ScreenshotService {

        private String observedScreenshotName;

        private RecordingScreenshotService() {
            super(Path.of("target", "screenshots"));
        }

        @Override
        public String capture(WebDriver driver, String screenshotName) {
            observedScreenshotName = screenshotName;
            return "target/screenshots/manual.png";
        }

        @Override
        public List<String> captureObjectInParts(
                WebDriver driver,
                WebElement element,
                ResolvedStepContext step,
                String screenshotName
        ) {
            observedScreenshotName = screenshotName;
            return List.of("target/screenshots/object-part-1.png", "target/screenshots/object-part-2.png");
        }

        @Override
        public List<String> captureFullPageInParts(
                WebDriver driver,
                ResolvedStepContext step,
                String screenshotName
        ) {
            observedScreenshotName = screenshotName;
            return List.of("target/screenshots/full-part-1.png", "target/screenshots/full-part-2.png");
        }
    }

}
