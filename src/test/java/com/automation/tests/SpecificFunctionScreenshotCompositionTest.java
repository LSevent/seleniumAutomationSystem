package com.automation.tests;

import com.automation.config.ExcelExecutionConfig;
import com.automation.context.EvidenceContextHolder;
import com.automation.context.ScreenshotContextHolder;
import com.automation.context.StepContextHolder;
import com.automation.engine.KeywordEngine;
import com.automation.engine.KeywordResolver;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.models.ExecutionResult;
import com.automation.models.KeywordSourceType;
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
public class SpecificFunctionScreenshotCompositionTest {

    private static final Path TEMPLATE_FILE = Path.of(
            "src", "test", "resources", "testdata", "Template Testing.xlsx"
    );

    private ExcelReader excelReader;

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        ScreenshotContextHolder.clear();
        EvidenceContextHolder.clear();
        StepContextHolder.clear();
        if (excelReader != null) {
            excelReader.close();
            excelReader = null;
        }
    }

    @Test
    public void specificFunctionShouldComposeManualScreenshotEvidence() {
        FakeWebDriver driver = driver();
        RecordingScreenshotService screenshotService = new RecordingScreenshotService();
        ResolvedStepContext step = step(
                "captureScreenshotEvidence",
                "",
                "Value should not label screenshot",
                "Value should not label screenshot",
                "",
                ""
        );

        ExecutionResult result = engine(driver, screenshotService).execute(step);

        Assert.assertTrue(result.isSuccess(), result.getMessage());
        Assert.assertEquals(result.getExecutionSource(), KeywordSourceType.SPECIFIC.name());
        Assert.assertEquals(
                result.getExecutedByClass(),
                "com.automation.functions.DEMO.SpecificFunction"
        );
        Assert.assertEquals(result.getEvidence(), "target/screenshots/specific-manual.png");
        Assert.assertEquals(screenshotService.observedScreenshotName, "25D_Specific composed screenshot testcase_1");
        Assert.assertFalse(screenshotService.observedScreenshotName.contains("Value should not label screenshot"));
        Assert.assertTrue(EvidenceContextHolder.getAll().isEmpty());
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    @Test
    public void specificFunctionShouldComposeObjectScreenshotEvidence() {
        FakeWebDriver driver = driver();
        RecordingScreenshotService screenshotService = new RecordingScreenshotService();
        ResolvedStepContext step = step(
                "captureObjectScreenshotEvidence",
                "pnlEvidence",
                "Value should not label object screenshot",
                "Value should not label object screenshot",
                "//section[@id='evidence']",
                "//section[@id='evidence']"
        );

        ExecutionResult result = engine(driver, screenshotService).execute(step);

        Assert.assertTrue(result.isSuccess(), result.getMessage());
        Assert.assertEquals(result.getExecutionSource(), KeywordSourceType.SPECIFIC.name());
        Assert.assertEquals(
                result.getExecutedByClass(),
                "com.automation.functions.DEMO.SpecificFunction"
        );
        Assert.assertEquals(
                result.getEvidence(),
                "target/screenshots/specific-object-1.png"
                        + System.lineSeparator()
                        + "target/screenshots/specific-object-2.png"
        );
        Assert.assertTrue(screenshotService.observedScreenshotName.contains("Specific composed screenshot"));
        Assert.assertFalse(screenshotService.observedScreenshotName.contains("Value should not label object screenshot"));
        Assert.assertTrue(EvidenceContextHolder.getAll().isEmpty());
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    @Test
    public void collectedEvidenceShouldRemainOnResultWhenSpecificKeywordFails() {
        FakeWebDriver driver = driver();
        RecordingScreenshotService screenshotService = new RecordingScreenshotService();
        ResolvedStepContext step = step(
                "captureScreenshotThenFail",
                "",
                "",
                "",
                "",
                ""
        );

        ExecutionResult result = engine(driver, screenshotService).execute(step);

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(result.getEvidence(), "target/screenshots/specific-manual.png");
        Assert.assertTrue(result.getMessage().contains("Intentional failure after screenshot evidence."));
        Assert.assertTrue(EvidenceContextHolder.getAll().isEmpty());
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    private KeywordEngine engine(FakeWebDriver driver, ScreenshotService screenshotService) {
        excelReader = new ExcelReader(TEMPLATE_FILE.toString());
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        return new KeywordEngine(
                dataReader,
                objectRepositoryReader,
                new KeywordResolver(driver.driver()),
                new ExcelReportConfig(false, true, true),
                ExcelExecutionConfig.load(),
                screenshotService
        );
    }

    private FakeWebDriver driver() {
        FakeWebDriver driver = new FakeWebDriver();
        driver.addElement("//section[@id='evidence']", "Evidence panel");
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
                .scenarioNo("25D")
                .scenarioAction("Specific Screenshot Composition")
                .scenarioName("SpecificFunction screenshot composition")
                .sheetName("Specific Screenshot Composition")
                .testcaseName("Specific composed screenshot testcase")
                .testcaseParentRow(4)
                .excelRow(7)
                .stepNumber(1)
                .keyword(keyword)
                .objectName(objectName)
                .application("DEMO")
                .description("Specific composed screenshot")
                .rawValue(rawValue)
                .resolvedValue(resolvedValue)
                .rawXPath(rawXPath)
                .resolvedXPath(resolvedXPath)
                .executedBy("")
                .build();
    }

    private static final class RecordingScreenshotService extends ScreenshotService {

        private String observedScreenshotName;

        private RecordingScreenshotService() {
            super(Path.of("target", "screenshots"));
        }

        @Override
        public String capture(WebDriver driver, String screenshotName) {
            observedScreenshotName = screenshotName;
            return "target/screenshots/specific-manual.png";
        }

        @Override
        public String capture(WebDriver driver, String screenshotName, String screenshotType) {
            observedScreenshotName = screenshotName;
            return "target/screenshots/specific-manual.png";
        }

        @Override
        public List<String> captureObjectInParts(
                WebDriver driver,
                WebElement element,
                ResolvedStepContext step,
                String screenshotName
        ) {
            observedScreenshotName = screenshotName;
            return List.of(
                    "target/screenshots/specific-object-1.png",
                    "target/screenshots/specific-object-2.png"
            );
        }
    }
}
