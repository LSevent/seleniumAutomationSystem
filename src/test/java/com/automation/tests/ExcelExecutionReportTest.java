package com.automation.tests;

import com.automation.engine.FunctionResolver;
import com.automation.engine.KeywordEngine;
import com.automation.engine.ScenarioRunner;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.ExecutionResult;
import com.automation.models.Scenario;
import com.automation.models.TestStep;
import com.automation.reports.ExcelExecutionReporter;
import com.automation.reports.ExcelReportConfig;
import com.automation.tests.support.ExcelKeywordTestWorkbookFactory;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Test(singleThreaded = true)
public class ExcelExecutionReportTest {

    private static final Path TEMP_DIR = Path.of("target", "excel-execution-report-test");
    private static final Path LOCAL_HTML = Path.of("src", "test", "resources", "test-pages", "excel-keyword-test.html");
    private static final Path SCREENSHOT_DIR = Path.of("test-output", "screenshots");

    private Path workbookPath;
    private String baseUrl;

    @BeforeClass
    public void createWorkbook() throws IOException {
        baseUrl = LOCAL_HTML.toAbsolutePath().toUri().toString();
        workbookPath = ExcelKeywordTestWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("excel-execution-report-test.xlsx"),
                baseUrl
        );
    }

    @Test(priority = 1)
    public void excelExecutionReportShouldContainStepDetailsAndManualScreenshots() throws IOException {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ScenarioRunner runner = scenarioRunner(
                    excelReader,
                    fakeDriver,
                    new ExcelReportConfig(false, true, true)
            );

            List<ExecutionResult> results = runner.runActiveScenarios();

            Assert.assertTrue(results.stream().allMatch(ExecutionResult::isSuccess), failureMessages(results));

            List<String> manualScreenshots = results.stream()
                    .filter(result -> "screenshot".equalsIgnoreCase(result.getFunctionName()))
                    .map(ExecutionResult::getEvidence)
                    .filter(evidence -> evidence.toLowerCase().endsWith(".png"))
                    .toList();

            Assert.assertEquals(manualScreenshots.size(), 3);
            manualScreenshots.forEach(path -> Assert.assertTrue(Files.exists(Path.of(path)), "Screenshot should exist: " + path));

            Path reportPath = Path.of(ExcelExecutionReporter.getReportFilePath());
            Assert.assertTrue(Files.exists(reportPath), "HTML report should be created.");

            String reportHtml = Files.readString(reportPath);
            Assert.assertTrue(reportHtml.contains("Scenario: [1] Local keyword execution test"));
            Assert.assertTrue(reportHtml.contains("Testcase: Login BRS"));
            Assert.assertTrue(reportHtml.contains("Testcase: Create Booking"));
            Assert.assertTrue(reportHtml.contains("Summary"));
            Assert.assertTrue(reportHtml.contains("Steps"));
            Assert.assertFalse(reportHtml.contains("Scenario Summary"));
            Assert.assertFalse(reportHtml.contains("Testcase Summary"));
            Assert.assertFalse(reportHtml.contains("Step Table"));
            Assert.assertTrue(reportHtml.contains("Scenario ACTION"));
            Assert.assertTrue(reportHtml.contains("Scenario status") || reportHtml.contains("Status"));
            Assert.assertTrue(reportHtml.contains("Duration"));
            Assert.assertTrue(reportHtml.contains("Step"));
            Assert.assertTrue(reportHtml.contains("Excel Row"));
            Assert.assertTrue(reportHtml.contains("Description"));
            Assert.assertTrue(reportHtml.contains("Function"));
            Assert.assertTrue(reportHtml.contains("Object"));
            Assert.assertTrue(reportHtml.contains("Application"));
            Assert.assertTrue(reportHtml.contains("Raw Value"));
            Assert.assertTrue(reportHtml.contains("Resolved Value"));
            Assert.assertTrue(reportHtml.contains("Raw XPath"));
            Assert.assertTrue(reportHtml.contains("Resolved XPath"));
            Assert.assertTrue(reportHtml.contains("Executed By"));
            Assert.assertTrue(reportHtml.contains("Status"));
            Assert.assertTrue(reportHtml.contains("Evidence"));
            Assert.assertTrue(reportHtml.contains("LOGIN_DATA.USERNAME"));
            Assert.assertTrue(reportHtml.contains("brs_admin"));
            Assert.assertTrue(reportHtml.contains("LOGIN_DATA.PASSWORD"));
            Assert.assertTrue(reportHtml.contains("****"));
            Assert.assertFalse(reportHtml.contains("brs123"), "Resolved password should be masked.");
            Assert.assertTrue(reportHtml.contains("file:///.../excel-keyword-test.html"));
            Assert.assertFalse(reportHtml.contains(baseUrl), "Resolved CONFIG.BASE_URL should be shortened in the report.");
            Assert.assertTrue(reportHtml.contains("BOOKING_DATA.ROOM_NAME"));
            Assert.assertTrue(reportHtml.contains("Meeting Room A"));
            Assert.assertTrue(reportHtml.contains("//button[contains(text(),&#39;{ROOM_NAME}&#39;)]"));
            Assert.assertTrue(reportHtml.contains("//button[contains(text(),&#39;Meeting Room A&#39;)]"));
            Assert.assertTrue(reportHtml.contains("BaseFunction.input"));
            Assert.assertTrue(reportHtml.contains("SpecificFunction.click"));
            Assert.assertTrue(reportHtml.contains("Manual screenshot"));
            Assert.assertTrue(reportHtml.contains("Evidence Gallery"));
            Assert.assertTrue(reportHtml.contains("After input title"));
            Assert.assertTrue(reportHtml.contains("After select room"));
            Assert.assertTrue(reportHtml.contains("After submit"));
            Assert.assertTrue(countOccurrences(reportHtml, "Manual screenshot:") >= 3);
            Assert.assertFalse(reportHtml.contains("RUNNING"), "Report should only render final scenario/testcase metadata.");
            Assert.assertFalse(reportHtml.contains("Step 1 passed:"), "Passed steps should be represented by the step table.");
            Assert.assertFalse(reportHtml.contains("Step 2 passed:"), "Passed steps should be represented by the step table.");
            Assert.assertFalse(reportHtml.contains("Step 3 passed:"), "Passed steps should be represented by the step table.");
            Assert.assertFalse(reportHtml.contains("Scenario finished successfully."));
            Assert.assertFalse(reportHtml.contains("Testcase finished successfully."));
            Assert.assertFalse(reportHtml.contains("Failure Summary"), "Passed scenario summary should not show an empty failure row.");
            Assert.assertFalse(reportHtml.contains("Message</th>"), "Passed testcase summary should not show an empty message row.");
            Assert.assertFalse(reportHtml.contains("ExcelReaderTest"), "Excel report should not be the generic TestNG method report.");
            Assert.assertFalse(reportHtml.contains("ScenarioReaderTest"), "Excel report should not be the generic TestNG method report.");
            Assert.assertFalse(reportHtml.contains("StepReaderTest"), "Excel report should not be the generic TestNG method report.");
            Assert.assertFalse(reportHtml.contains("DataReaderTest"), "Excel report should not be the generic TestNG method report.");
            Assert.assertFalse(reportHtml.contains("ObjectRepositoryReaderTest"), "Excel report should not be the generic TestNG method report.");
            Assert.assertFalse(reportHtml.contains("LoginTest"), "Excel report should not be the generic TestNG method report.");
            Assert.assertFalse(reportHtml.contains("DashboardTest"), "Excel report should not be the generic TestNG method report.");
        }
    }

    @Test(priority = 2)
    public void failureScreenshotShouldBeCreatedWhenEnabled() throws IOException {
        Path failureWorkbook = ExcelKeywordTestWorkbookFactory.createFailureWorkbook(
                TEMP_DIR.resolve("failure-screenshot-enabled.xlsx"),
                baseUrl,
                91,
                "Failing Keyword Test Enabled",
                "Failing keyword execution test enabled",
                "Failure With Screenshot"
        );
        String prefix = "91_Failure_With_Screenshot_step1_row5_Failure";
        Set<String> before = screenshotFilesStartingWith(prefix);

        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(failureWorkbook.toString())) {
            ScenarioRunner runner = scenarioRunner(
                    excelReader,
                    fakeDriver,
                    new ExcelReportConfig(false, true, true)
            );

            List<ExecutionResult> results = runner.runActiveScenarios();

            Assert.assertFalse(results.get(results.size() - 1).isSuccess());
        }

        Set<String> after = screenshotFilesStartingWith(prefix);
        after.removeAll(before);
        Assert.assertFalse(after.isEmpty(), "Failure screenshot should be created when enabled.");

        String reportHtml = Files.readString(Path.of(ExcelExecutionReporter.getReportFilePath()));
        Assert.assertTrue(reportHtml.contains("Failure screenshot"));
        Assert.assertTrue(reportHtml.contains("Failure Details"));
        Assert.assertTrue(reportHtml.contains("Error Message"));
        Assert.assertTrue(reportHtml.contains("Excel Row"));
        Assert.assertTrue(reportHtml.contains("Function"));
        Assert.assertTrue(reportHtml.contains("Object"));
        Assert.assertTrue(reportHtml.contains("Application"));
        Assert.assertTrue(reportHtml.contains("Evidence"));
        Assert.assertTrue(reportHtml.contains("Evidence Gallery"));
        Assert.assertFalse(reportHtml.contains("Scenario failed. Scenario failed."));
    }

    @Test(priority = 3)
    public void failureScreenshotShouldNotBeCreatedWhenDisabled() throws IOException {
        Path failureWorkbook = ExcelKeywordTestWorkbookFactory.createFailureWorkbook(
                TEMP_DIR.resolve("failure-screenshot-disabled.xlsx"),
                baseUrl,
                92,
                "Failing Keyword Test Disabled",
                "Failing keyword execution test disabled",
                "Failure Without Screenshot"
        );
        String prefix = "92_Failure_Without_Screenshot_step1_row5_Failure";
        Set<String> before = screenshotFilesStartingWith(prefix);

        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(failureWorkbook.toString())) {
            ScenarioRunner runner = scenarioRunner(
                    excelReader,
                    fakeDriver,
                    new ExcelReportConfig(false, false, true)
            );

            List<ExecutionResult> results = runner.runActiveScenarios();

            Assert.assertFalse(results.get(results.size() - 1).isSuccess());
        }

        Set<String> after = screenshotFilesStartingWith(prefix);
        after.removeAll(before);
        Assert.assertTrue(after.isEmpty(), "Failure screenshot should not be created when disabled.");
    }

    @Test(priority = 4)
    public void manualScreenshotDisabledShouldSkipWithoutFailing() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            Scenario scenario = new ScenarioReader(excelReader).getActiveScenarios().get(0);
            TestStep screenshotStep = new StepReader(excelReader).getActiveSteps(scenario).stream()
                    .filter(step -> "screenshot".equalsIgnoreCase(step.getFunction()))
                    .findFirst()
                    .orElseThrow();
            KeywordEngine keywordEngine = keywordEngine(
                    excelReader,
                    fakeDriver,
                    new ExcelReportConfig(false, true, false)
            );

            ExecutionResult result = keywordEngine.executeStep(scenario, screenshotStep);

            Assert.assertTrue(result.isSuccess());
            Assert.assertEquals(result.getStatus(), ExecutionResult.STATUS_SKIP);
            Assert.assertEquals(result.getEvidence(), "Manual screenshot skipped because report.manualScreenshotEnabled=false.");
        }
    }

    private ScenarioRunner scenarioRunner(ExcelReader excelReader, FakeWebDriver fakeDriver, ExcelReportConfig reportConfig) {
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        FunctionResolver functionResolver = new FunctionResolver(fakeDriver.driver());
        KeywordEngine keywordEngine = new KeywordEngine(dataReader, objectRepositoryReader, functionResolver, reportConfig);
        ExcelExecutionReporter reporter = new ExcelExecutionReporter(fakeDriver.driver(), reportConfig);
        return new ScenarioRunner(new ScenarioReader(excelReader), new StepReader(excelReader), keywordEngine, reporter);
    }

    private KeywordEngine keywordEngine(ExcelReader excelReader, FakeWebDriver fakeDriver, ExcelReportConfig reportConfig) {
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        FunctionResolver functionResolver = new FunctionResolver(fakeDriver.driver());
        return new KeywordEngine(dataReader, objectRepositoryReader, functionResolver, reportConfig);
    }

    private Set<String> screenshotFilesStartingWith(String prefix) throws IOException {
        if (!Files.exists(SCREENSHOT_DIR)) {
            return Set.of();
        }
        Set<String> fileNames = new HashSet<>();
        try (var files = Files.list(SCREENSHOT_DIR)) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.startsWith(prefix))
                    .forEach(fileNames::add);
        }
        return fileNames;
    }

    private String failureMessages(List<ExecutionResult> results) {
        return results.stream()
                .filter(result -> !result.isSuccess())
                .map(ExecutionResult::getMessage)
                .toList()
                .toString();
    }

    private int countOccurrences(String value, String searchText) {
        int count = 0;
        int index = value.indexOf(searchText);
        while (index >= 0) {
            count++;
            index = value.indexOf(searchText, index + searchText.length());
        }
        return count;
    }

    private FakeWebDriver localPageDriver() {
        FakeWebDriver fakeDriver = new FakeWebDriver();
        fakeDriver.setTitle("Excel Keyword Test");
        fakeDriver.addElement("//input[@id='username']", "");
        fakeDriver.addElement("//input[@id='password']", "");
        fakeDriver.addElement("//button[@id='loginButton']", "Login");
        fakeDriver.addElement("//h1[@id='dashboard']", "Dashboard");
        fakeDriver.addElement("//input[@id='bookingTitle']", "");
        fakeDriver.addElement("//button[contains(text(),'Meeting Room A')]", "Meeting Room A");
        fakeDriver.addElement("//div[@id='message']", "Booking created successfully");
        return fakeDriver;
    }
}
