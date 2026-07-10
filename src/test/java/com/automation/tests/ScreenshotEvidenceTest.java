package com.automation.tests;

import com.automation.models.ResolvedStepContext;
import com.automation.services.ScreenshotEvidence;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class ScreenshotEvidenceTest {

    @Test
    public void supportsShouldRecognizeScreenshotKeywordsCaseInsensitively() {
        Assert.assertTrue(ScreenshotEvidence.supportsKeyword("screenshot"));
        Assert.assertTrue(ScreenshotEvidence.supportsKeyword(" SCREENSHOT "));
        Assert.assertTrue(ScreenshotEvidence.supportsKeyword("screenshotPartByObject"));
        Assert.assertTrue(ScreenshotEvidence.supportsKeyword(" SCREENSHOTPARTBYOBJECT "));
        Assert.assertFalse(ScreenshotEvidence.supportsKeyword("click"));
        Assert.assertFalse(ScreenshotEvidence.supportsKeyword(""));
        Assert.assertFalse(ScreenshotEvidence.supportsKeyword(null));
    }

    @Test
    public void labelsShouldUseDescriptionBeforeValueOrObjectName() {
        ResolvedStepContext step = step("screenshotPartByObject", "pnlBooking", "Sensitive value", "Evidence label");

        Assert.assertEquals(ScreenshotEvidence.manualLabel(step), "Evidence label");
        Assert.assertEquals(ScreenshotEvidence.objectLabel(step), "Evidence label");
    }

    @Test
    public void objectLabelShouldFallbackToObjectNameWhenDescriptionIsBlank() {
        ResolvedStepContext step = step("screenshotPartByObject", "pnlBooking", "Sensitive value", "");

        Assert.assertEquals(ScreenshotEvidence.objectLabel(step), "pnlBooking");
    }

    @Test
    public void baseNameShouldUseStepContextAndLabel() {
        ResolvedStepContext step = step("screenshot", "", "Value should not label screenshot", "After login");

        Assert.assertEquals(
                ScreenshotEvidence.baseName(step, ScreenshotEvidence.manualLabel(step)),
                "25E_Login BRS_step3_row9_After login"
        );
    }

    @Test
    public void evidenceShouldRenderSingleOrMultiplePathsWithUnavailableFallback() {
        Assert.assertEquals(
                ScreenshotEvidence.evidenceOrUnavailable("target/screenshots/manual.png", "missing"),
                "target/screenshots/manual.png"
        );
        Assert.assertEquals(
                ScreenshotEvidence.evidenceOrUnavailable(List.of("part-1.png", "part-2.png"), "missing"),
                "part-1.png" + System.lineSeparator() + "part-2.png"
        );
        Assert.assertEquals(ScreenshotEvidence.evidenceOrUnavailable("", "missing"), "missing");
        Assert.assertEquals(ScreenshotEvidence.evidenceOrUnavailable(List.of(), "missing"), "missing");
    }

    private ResolvedStepContext step(
            String keyword,
            String objectName,
            String resolvedValue,
            String description
    ) {
        return ResolvedStepContext.builder()
                .scenarioNo("25E")
                .scenarioAction("Screenshot Handler Consolidation")
                .scenarioName("Screenshot Handler Consolidation")
                .sheetName("Screenshot Handler Consolidation")
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
}
