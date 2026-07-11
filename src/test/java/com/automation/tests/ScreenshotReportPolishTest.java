package com.automation.tests;

import com.automation.config.ExcelExecutionConfig;
import com.automation.engine.KeywordEngine;
import com.automation.models.ExecutionResult;
import com.automation.models.ResolvedStepContext;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.reports.ExcelExecutionReporter;
import com.automation.reports.ExcelReportConfig;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

@Test(singleThreaded = true)
public class ScreenshotReportPolishTest {

    private static final Path TEMPLATE = Path.of("src", "test", "resources", "testdata", "Template Testing.xlsx");
    private static final Path TEMP_DIR = Path.of("target", "phase-26e-report-polish");

    @Test
    public void failureShouldPreserveCollectedEvidenceAndAppendFailureScreenshot() throws IOException {
        ExcelExecutionConfig config = executionConfig("combined-failure-evidence");
        Files.createDirectories(config.getScreenshotOutputDirectory());
        Path collectedEvidence = config.getScreenshotOutputDirectory().resolve("before-failure.png");
        Files.write(collectedEvidence, new byte[]{1});

        ExcelExecutionReporter reporter = reporter(config);
        ResolvedStepContext step = step("captureThenFail", "Evidence captured before failure");
        ExecutionResult result = ExecutionResult.fromStep(step)
                .executedByClass(KeywordEngine.class.getName())
                .executionSource("SPECIFIC")
                .success(false)
                .status(ExecutionResult.STATUS_FAIL)
                .evidence(collectedEvidence.toString())
                .message("Failure after collecting evidence.")
                .build();

        writeReport(reporter, result, false);

        String reportHtml = Files.readString(config.getReportFilePath());
        Assert.assertTrue(reportHtml.contains("before-failure.png"));
        Assert.assertTrue(reportHtml.contains("Evidence captured before failure"));
        Assert.assertTrue(reportHtml.contains("Failure screenshot"));
        Assert.assertTrue(reportHtml.contains("Evidence Gallery"));
        Assert.assertTrue(reportHtml.contains("Failure Details"));
    }

    @Test
    public void fullPageEvidenceShouldUseExplicitGalleryLabels() throws IOException {
        ExcelExecutionConfig config = executionConfig("full-page-labels");
        Files.createDirectories(config.getScreenshotOutputDirectory());
        Path partOne = config.getScreenshotOutputDirectory().resolve("full-page-part-1.png");
        Path partTwo = config.getScreenshotOutputDirectory().resolve("full-page-part-2.png");
        Files.write(partOne, new byte[]{1});
        Files.write(partTwo, new byte[]{2});

        ExcelExecutionReporter reporter = reporter(config);
        ResolvedStepContext step = step("screenshotFullPart", "Entire booking page");
        ExecutionResult result = ExecutionResult.success(
                step,
                KeywordEngine.class.getName(),
                "BASE",
                partOne + System.lineSeparator() + partTwo,
                "Full page screenshot captured."
        );

        writeReport(reporter, result, true);

        String reportHtml = Files.readString(config.getReportFilePath());
        Assert.assertTrue(reportHtml.contains("Full-page screenshot: Entire booking page part 1"));
        Assert.assertTrue(reportHtml.contains("Full-page screenshot: Entire booking page part 2"));
        Assert.assertTrue(reportHtml.contains("full-page-part-1.png"));
        Assert.assertTrue(reportHtml.contains("full-page-part-2.png"));
    }

    private ExcelExecutionReporter reporter(ExcelExecutionConfig config) {
        return new ExcelExecutionReporter(
                new FakeWebDriver().driver(),
                new ExcelReportConfig(false, true, true),
                config
        );
    }

    private void writeReport(ExcelExecutionReporter reporter, ExecutionResult result, boolean success) {
        Scenario scenario = new Scenario("26E", true, "Screenshot Polish", "Screenshot polish", 1);
        TestCaseBlock testcase = new TestCaseBlock(
                "26E",
                "Screenshot polish",
                "Screenshot Polish",
                "Screenshot report polish",
                true,
                "BRS",
                "Screenshot report polish",
                2
        );
        reporter.startScenario(scenario);
        reporter.startTestCase(testcase);
        reporter.logStep(result);
        reporter.finishTestCase(testcase, success, success ? "Passed." : "Failed as expected.");
        reporter.finishScenario(scenario, success, success ? "Passed." : "Failed as expected.");
        reporter.flush();
    }

    private ExcelExecutionConfig executionConfig(String name) {
        Properties properties = new Properties();
        properties.setProperty(ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY, TEMPLATE.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, TEMP_DIR.resolve(name).toString());
        return ExcelExecutionConfig.fromProperties(properties, Map.of());
    }

    private ResolvedStepContext step(String keyword, String description) {
        return ResolvedStepContext.builder()
                .scenarioNo("26E")
                .scenarioAction("Screenshot Polish")
                .scenarioName("Screenshot polish")
                .sheetName("Screenshot Polish")
                .testcaseName("Screenshot report polish")
                .testcaseParentRow(2)
                .excelRow(3)
                .stepNumber(1)
                .keyword(keyword)
                .objectName("")
                .application("BRS")
                .description(description)
                .rawValue("")
                .resolvedValue("")
                .rawXPath("")
                .resolvedXPath("")
                .executedBy(KeywordEngine.class.getName())
                .build();
    }
}
