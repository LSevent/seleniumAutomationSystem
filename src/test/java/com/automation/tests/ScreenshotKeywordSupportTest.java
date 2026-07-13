package com.automation.tests;

import com.automation.base.BaseFunction;
import com.automation.models.ResolvedStepContext;
import com.automation.services.ScreenshotService;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;

public class ScreenshotKeywordSupportTest {

    @Test
    public void screenshotKeywordsShouldBeNormalBaseFunctionKeywords() throws NoSuchMethodException {
        Method screenshot = BaseFunction.class.getMethod("screenshot");
        Method screenshotPartByObject = BaseFunction.class.getMethod("screenshotPartByObject");
        Method screenshotFullPart = BaseFunction.class.getMethod("screenshotFullPart");

        Assert.assertEquals(screenshot.getParameterCount(), 0);
        Assert.assertEquals(screenshotPartByObject.getParameterCount(), 0);
        Assert.assertEquals(screenshotFullPart.getParameterCount(), 0);
    }

    @Test
    public void serviceLabelsShouldUseDescriptionBeforeValueOrObjectName() {
        ScreenshotService service = new ScreenshotService(Path.of("target", "screenshots"));
        ResolvedStepContext step = step("screenshotPartByObject", "pnlBooking", "Sensitive value", "Evidence label");

        Assert.assertEquals(service.manualLabel(step), "Evidence label");
        Assert.assertEquals(service.objectLabel(step), "Evidence label");
    }

    @Test
    public void serviceObjectLabelShouldFallbackToObjectNameWhenDescriptionIsBlank() {
        ScreenshotService service = new ScreenshotService(Path.of("target", "screenshots"));
        ResolvedStepContext step = step("screenshotPartByObject", "pnlBooking", "Sensitive value", "");

        Assert.assertEquals(service.objectLabel(step), "pnlBooking");
    }

    @Test
    public void serviceFullPageLabelShouldUseDescriptionThenFallback() {
        ScreenshotService service = new ScreenshotService(Path.of("target", "screenshots"));

        Assert.assertEquals(
                service.fullPageLabel(step("screenshotFullPart", "", "Sensitive value", "Full page evidence")),
                "Full page evidence"
        );
        Assert.assertEquals(
                service.fullPageLabel(step("screenshotFullPart", "", "Sensitive value", "")),
                "FullPageScreenshot"
        );
    }

    @Test
    public void serviceScreenshotNameShouldUseStepContextAndLabelWithoutResolvedValue() {
        NamingProbeScreenshotService service = new NamingProbeScreenshotService();
        ResolvedStepContext step = step("screenshot", "", "Value should not label screenshot", "After login");

        service.captureScreen(null, step, service.manualLabel(step));

        Assert.assertEquals(service.observedScreenshotName, "25F_Login BRS_3");
        Assert.assertFalse(service.observedScreenshotName.contains("Value should not label screenshot"));
    }

    private ResolvedStepContext step(
            String keyword,
            String objectName,
            String resolvedValue,
            String description
    ) {
        return ResolvedStepContext.builder()
                .scenarioNo("25F")
                .scenarioAction("Screenshot Keyword Support")
                .scenarioName("Screenshot Keyword Support")
                .sheetName("Screenshot Keyword Support")
                .testcaseName("Login BRS")
                .testcaseParentRow(4)
                .excelRow(9)
                .stepNumber(3)
                .keyword(keyword)
                .objectName(objectName)
                .application("BRS")
                .description(description)
                .rawValue(resolvedValue)
                .resolvedValue(resolvedValue)
                .rawXPath("//section[@id='booking']")
                .resolvedXPath("//section[@id='booking']")
                .executedBy("")
                .build();
    }

    private static final class NamingProbeScreenshotService extends ScreenshotService {

        private String observedScreenshotName;

        private NamingProbeScreenshotService() {
            super(Path.of("target", "screenshots"));
        }

        @Override
        public String capture(WebDriver driver, String screenshotName) {
            observedScreenshotName = screenshotName;
            return "target/screenshots/manual.png";
        }

        @Override
        public String capture(WebDriver driver, String screenshotName, String screenshotType) {
            observedScreenshotName = screenshotName;
            return "target/screenshots/manual.png";
        }
    }
}
