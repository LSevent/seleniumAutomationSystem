package com.automation.tests;

import com.automation.config.ExcelExecutionConfig;
import com.automation.engine.ExecutionPlanBuilder;
import com.automation.engine.KeywordResolver;
import com.automation.engine.KeywordEngine;
import com.automation.engine.ScenarioRunner;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.ExecutionResult;
import com.automation.models.ResolvedStepContext;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.reports.ExcelExecutionReporter;
import com.automation.reports.ExcelReportConfig;
import com.automation.tests.support.ExcelKeywordTestWorkbookFactory;
import com.automation.tests.support.FakeWebDriver;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static com.automation.tests.support.ValidationWorkbookFactory.objectRepository;
import static com.automation.tests.support.ValidationWorkbookFactory.scenarioSheet;
import static com.automation.tests.support.ValidationWorkbookFactory.scenarios;
import static com.automation.tests.support.ValidationWorkbookFactory.sheet;

@Test(singleThreaded = true)
public class ExcelExecutionReportTest {

    private static final Path TEMP_DIR = Path.of("target", "excel-execution-report-test");
    private static final Path LOCAL_HTML = Path.of("src", "test", "resources", "test-pages", "excel-keyword-test.html");

    private Path workbookPath;
    private Path screenshotDirectory;
    private ExcelExecutionConfig executionConfig;
    private String baseUrl;

    @BeforeClass
    public void createWorkbook() throws IOException {
        baseUrl = LOCAL_HTML.toAbsolutePath().toUri().toString();
        workbookPath = ExcelKeywordTestWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("excel-execution-report-test.xlsx"),
                baseUrl
        );
        executionConfig = executionConfig(workbookPath);
        screenshotDirectory = executionConfig.getScreenshotOutputDirectory();
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
                    .filter(result -> "screenshot".equalsIgnoreCase(result.getKeywordName()))
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
            Assert.assertTrue(reportHtml.contains("Keyword"));
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
            Assert.assertTrue(reportHtml.contains("Screenshots/"));
            Assert.assertTrue(reportHtml.contains("Capture form after title"));
            Assert.assertTrue(reportHtml.contains("Capture selected room"));
            Assert.assertTrue(reportHtml.contains("Capture submit result"));
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
        executionConfig = executionConfig(failureWorkbook);
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
        Assert.assertTrue(reportHtml.contains("Keyword"));
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
        executionConfig = executionConfig(failureWorkbook);
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
        executionConfig = executionConfig(workbookPath);
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ResolvedStepContext screenshotStep = resolvedSteps(excelReader).stream()
                    .filter(step -> "screenshot".equalsIgnoreCase(step.getKeyword()))
                    .findFirst()
                    .orElseThrow();
            KeywordEngine keywordEngine = keywordEngine(
                    excelReader,
                    fakeDriver,
                    new ExcelReportConfig(false, true, false)
            );

            ExecutionResult result = keywordEngine.execute(screenshotStep);

            Assert.assertTrue(result.isSuccess());
            Assert.assertEquals(result.getStatus(), ExecutionResult.STATUS_SKIP);
            Assert.assertEquals(result.getEvidence(), "Manual screenshot skipped because report.manualScreenshotEnabled=false.");
        }
    }

    @Test(priority = 5)
    public void flowRowsShouldShowReadableFlowDetails() throws IOException {
        Path flowWorkbook = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("flow-report-details.xlsx"),
                scenarios(new Object[][]{{1, "Y", "Flow Report", "Flow report detail test"}}),
                scenarioSheet("Flow Report", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "CONFIG.RUN_BOOKINGS = Yes", "", "Run booking loop condition"},
                        {"", "", "forEachDataRow", "", "BOOKING_DATA", "", "Start booking-data loop"},
                        {"", "", "input", "txtBookingTitle", "BOOKING_DATA.BOOKING_TITLE", "", "Input booking title"},
                        {"", "", "endForEachDataRow", "", "", "", "End booking-data loop"},
                        {"", "", "else", "", "", "", "Loop skipped fallback"},
                        {"", "", "click", "btnSkippedLoopFallback", "", "", "Fallback when loop skipped"},
                        {"", "", "endIf", "", "", "", "End condition"},
                        {"", "", "click", "btnAfterFlow", "", "", "After conditional loop"}
                }),
                sheet("CONFIG", new String[]{"NO", "RUN_BOOKINGS"}, new Object[][]{
                        {1, "Yes"}
                }),
                sheet("BOOKING_DATA", new String[]{"NO", "BOOKING_TITLE"}, new Object[][]{
                        {1, "Weekly Meeting"},
                        {1, "Daily Standup"}
                }),
                objectRepository(new Object[][]{
                        {"BRS", "txtBookingTitle", "//input[@id='bookingTitle']", "Booking title"},
                        {"BRS", "btnSkippedLoopFallback", "//button[@id='fallback']", "Loop skipped fallback"},
                        {"BRS", "btnAfterFlow", "//button[@id='after-flow']", "After flow"}
                })
        );

        executionConfig = executionConfig(flowWorkbook);
        FakeWebDriver fakeDriver = localPageDriver();
        fakeDriver.addElement("//button[@id='fallback']", "Fallback");
        fakeDriver.addElement("//button[@id='after-flow']", "After flow");

        try (ExcelReader excelReader = new ExcelReader(flowWorkbook.toString())) {
            ScenarioRunner runner = scenarioRunner(
                    excelReader,
                    fakeDriver,
                    new ExcelReportConfig(false, true, true)
            );

            List<ExecutionResult> results = runner.runActiveScenarios();

            Assert.assertTrue(results.stream().allMatch(ExecutionResult::isSuccess), failureMessages(results));
        }

        String reportHtml = Files.readString(Path.of(ExcelExecutionReporter.getReportFilePath()));
        Assert.assertTrue(reportHtml.contains(">Flow<"), "Flow rows should show a readable executor.");
        Assert.assertTrue(reportHtml.contains("Condition matched. Entering ifEquals branch."));
        Assert.assertTrue(reportHtml.contains("Starting data row loop. BOOKING_DATA row 1 of 2."));
        Assert.assertTrue(reportHtml.contains("Ended data row loop. BOOKING_DATA row 2 of 2."));
        Assert.assertTrue(reportHtml.contains("A previous conditional branch already matched. Skipping else branch."));
        Assert.assertTrue(reportHtml.contains("Skipped because the active conditional branch does not include this step."));
    }

    @Test(priority = 6)
    public void objectScreenshotPartsShouldRenderAsSeparateEvidenceItems() throws IOException {
        executionConfig = executionConfig(workbookPath);
        Files.createDirectories(executionConfig.getScreenshotOutputDirectory());
        Path partOne = executionConfig.getScreenshotOutputDirectory().resolve("object-part-1.png");
        Path partTwo = executionConfig.getScreenshotOutputDirectory().resolve("object-part-2.png");
        Files.write(partOne, new byte[]{1});
        Files.write(partTwo, new byte[]{2});

        FakeWebDriver fakeDriver = localPageDriver();
        ExcelExecutionReporter reporter = new ExcelExecutionReporter(
                fakeDriver.driver(),
                new ExcelReportConfig(false, true, true),
                executionConfig
        );
        Scenario scenario = new Scenario("1", true, "Evidence Flow", "Evidence scenario", 1);
        TestCaseBlock testCaseBlock = new TestCaseBlock(
                "1",
                "Evidence scenario",
                "Evidence Flow",
                "Object screenshot testcase",
                true,
                "BRS",
                "Object screenshot regression",
                2
        );
        ResolvedStepContext step = ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioAction("Evidence Flow")
                .scenarioName("Evidence scenario")
                .sheetName("Evidence Flow")
                .testcaseName("Object screenshot testcase")
                .testcaseParentRow(2)
                .excelRow(5)
                .stepNumber(1)
                .keyword("screenshotPartByObject")
                .objectName("pnlBooking")
                .application("BRS")
                .description("Capture booking panel")
                .rawValue("Booking panel")
                .resolvedValue("Booking panel")
                .rawXPath("//section[@id='booking']")
                .resolvedXPath("//section[@id='booking']")
                .executedBy(KeywordEngine.class.getName())
                .build();
        ExecutionResult result = ExecutionResult.success(
                step,
                KeywordEngine.class.getName(),
                "REPORT",
                partOne + System.lineSeparator() + partTwo,
                "Object screenshot captured in 2 part(s)."
        );

        reporter.startScenario(scenario);
        reporter.startTestCase(testCaseBlock);
        reporter.logStep(result);
        reporter.finishTestCase(testCaseBlock, true, "Testcase passed.");
        reporter.finishScenario(scenario, true, "Scenario passed.");
        reporter.flush();

        String reportHtml = Files.readString(Path.of(ExcelExecutionReporter.getReportFilePath()));
        Assert.assertTrue(reportHtml.contains("Evidence Gallery"));
        Assert.assertTrue(reportHtml.contains("Object screenshot: Capture booking panel part 1"));
        Assert.assertTrue(reportHtml.contains("Object screenshot: Capture booking panel part 2"));
        Assert.assertTrue(reportHtml.contains("Screenshots/object-part-1.png"));
        Assert.assertTrue(reportHtml.contains("Screenshots/object-part-2.png"));
    }

    private ScenarioRunner scenarioRunner(ExcelReader excelReader, FakeWebDriver fakeDriver, ExcelReportConfig reportConfig) {
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        KeywordResolver keywordResolver = new KeywordResolver(fakeDriver.driver());
        KeywordEngine keywordEngine = new KeywordEngine(dataReader, objectRepositoryReader, keywordResolver, reportConfig, executionConfig);
        ExcelExecutionReporter reporter = new ExcelExecutionReporter(fakeDriver.driver(), reportConfig, executionConfig);
        return new ScenarioRunner(new ScenarioReader(excelReader), new StepReader(excelReader), keywordEngine, reporter);
    }

    private KeywordEngine keywordEngine(ExcelReader excelReader, FakeWebDriver fakeDriver, ExcelReportConfig reportConfig) {
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        KeywordResolver keywordResolver = new KeywordResolver(fakeDriver.driver());
        return new KeywordEngine(dataReader, objectRepositoryReader, keywordResolver, reportConfig, executionConfig);
    }

    private List<ResolvedStepContext> resolvedSteps(ExcelReader excelReader) {
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        return new ExecutionPlanBuilder(
                new ScenarioReader(excelReader),
                new StepReader(excelReader),
                dataReader,
                objectRepositoryReader
        ).build()
                .stream()
                .flatMap(scenario -> scenario.getTestcases().stream())
                .flatMap(testcase -> testcase.getSteps().stream())
                .toList();
    }

    private Set<String> screenshotFilesStartingWith(String prefix) throws IOException {
        if (!Files.exists(screenshotDirectory)) {
            return Set.of();
        }
        Set<String> fileNames = new HashSet<>();
        try (var files = Files.list(screenshotDirectory)) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.startsWith(prefix))
                    .forEach(fileNames::add);
        }
        return fileNames;
    }

    private ExcelExecutionConfig executionConfig(Path scenarioFilePath) {
        Properties properties = new Properties();
        properties.setProperty(ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY, scenarioFilePath.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, TEMP_DIR.resolve("reports").toString());
        return ExcelExecutionConfig.fromProperties(properties, java.util.Map.of());
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
