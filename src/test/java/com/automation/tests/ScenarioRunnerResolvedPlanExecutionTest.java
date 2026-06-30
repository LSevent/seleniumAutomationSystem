package com.automation.tests;

import com.automation.config.ExcelExecutionConfig;
import com.automation.engine.KeywordResolver;
import com.automation.engine.KeywordEngine;
import com.automation.engine.ScenarioRunner;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ExecutionResult;
import com.automation.models.ResolvedObject;
import com.automation.models.ResolvedStepContext;
import com.automation.models.Scenario;
import com.automation.models.TestStep;
import com.automation.reports.ExcelReportConfig;
import com.automation.tests.support.ExcelKeywordTestWorkbookFactory;
import com.automation.tests.support.FakeWebDriver;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Test(singleThreaded = true)
public class ScenarioRunnerResolvedPlanExecutionTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-13c4-resolved-plan");
    private static final Path LOCAL_HTML = Path.of(
            "src", "test", "resources", "test-pages", "excel-keyword-test.html"
    );

    private Path workbookPath;
    private String baseUrl;

    @BeforeClass
    public void createWorkbook() throws IOException {
        baseUrl = LOCAL_HTML.toAbsolutePath().toUri().toString();
        workbookPath = ExcelKeywordTestWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("resolved-plan-execution.xlsx"),
                baseUrl
        );
    }

    @Test
    public void shouldBuildValidateAndExecuteEachResolvedStepWithoutRuntimeResolution() {
        AtomicBoolean runtimeStarted = new AtomicBoolean();
        AtomicInteger valueResolutionCount = new AtomicInteger();
        AtomicInteger objectResolutionCount = new AtomicInteger();
        AtomicReference<RecordingKeywordEngine> engineReference = new AtomicReference<>();
        FakeWebDriver driver = driver();

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            CountingDataReader dataReader = new CountingDataReader(
                    excelReader,
                    runtimeStarted,
                    valueResolutionCount
            );
            CountingObjectRepositoryReader objectRepositoryReader = new CountingObjectRepositoryReader(
                    excelReader,
                    dataReader,
                    runtimeStarted,
                    objectResolutionCount
            );
            ExcelExecutionConfig executionConfig = executionConfig(workbookPath, "resolved-plan-report.html");
            ScenarioRunner runner = new ScenarioRunner(
                    new ScenarioReader(excelReader),
                    new StepReader(excelReader),
                    dataReader,
                    objectRepositoryReader,
                    () -> {
                        runtimeStarted.set(true);
                        RecordingKeywordEngine engine = new RecordingKeywordEngine(
                                dataReader,
                                objectRepositoryReader,
                                new KeywordResolver(driver.driver()),
                                executionConfig
                        );
                        engineReference.set(engine);
                        return engine;
                    }
            );

            List<ExecutionResult> results = runner.runActiveScenarios();

            Assert.assertTrue(runtimeStarted.get());
            Assert.assertTrue(valueResolutionCount.get() > 0);
            Assert.assertTrue(objectResolutionCount.get() > 0);
            Assert.assertEquals(results.size(), 11);
            Assert.assertTrue(results.stream().allMatch(ExecutionResult::isSuccess), failures(results));
            Assert.assertEquals(engineReference.get().executedSteps.size(), 11);
            Assert.assertEquals(
                    engineReference.get().executedSteps.stream().map(ResolvedStepContext::getKeyword).toList(),
                    results.stream().map(ExecutionResult::getKeywordName).toList()
            );
            Assert.assertTrue(engineReference.get().executedSteps.stream()
                    .allMatch(step -> "1".equals(step.getScenarioNo())));
            Assert.assertEquals(driver.getCurrentUrl(), baseUrl);
            Assert.assertEquals(driver.element("//input[@id='username']").getValue(), "brs_admin");
        }
    }

    @Test
    public void validationFailureShouldPreventRuntimeEngineStartup() throws IOException {
        Path invalidWorkbook = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("validation-before-runtime.xlsx"),
                ValidationWorkbookFactory.scenarios(new Object[][]{
                        {1, "Y", "Invalid Flow", "Validation must fail first"}
                }),
                ValidationWorkbookFactory.scenarioSheet("Invalid Flow", new Object[][]{
                        {"Invalid Testcase", "Y", "", "", "", "BRS", "Invalid testcase"},
                        {"", "", "openUrl", "", "", "", "Missing URL"}
                }),
                ValidationWorkbookFactory.objectRepository(new Object[][]{})
        );
        AtomicBoolean runtimeStarted = new AtomicBoolean();

        try (ExcelReader excelReader = new ExcelReader(invalidWorkbook.toString())) {
            DataReader dataReader = new DataReader(excelReader);
            ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
            ScenarioRunner runner = new ScenarioRunner(
                    new ScenarioReader(excelReader),
                    new StepReader(excelReader),
                    dataReader,
                    objectRepositoryReader,
                    () -> {
                        runtimeStarted.set(true);
                        return new KeywordEngine(
                                dataReader,
                                objectRepositoryReader,
                                new KeywordResolver(driver().driver()),
                                new ExcelReportConfig(false, true, true),
                                executionConfig(invalidWorkbook, "invalid-report.html")
                        );
                    }
            );

            FrameworkException exception = Assert.expectThrows(
                    FrameworkException.class,
                    runner::runActiveScenarios
            );

            Assert.assertTrue(exception.getMessage().contains("Pre-run validation failed"));
            Assert.assertFalse(runtimeStarted.get());
        }
    }

    @Test
    public void inactiveScenariosAndTestcasesShouldNotBeExecuted() throws IOException {
        Path inactiveWorkbook = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("inactive-plan-items.xlsx"),
                ValidationWorkbookFactory.scenarios(new Object[][]{
                        {1, "Y", "Active Flow", "Active scenario"},
                        {2, "N", "Inactive Flow", "Inactive scenario"}
                }),
                ValidationWorkbookFactory.scenarioSheet("Active Flow", new Object[][]{
                        {"Active Testcase", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "openUrl", "", "about:active", "", "Active step"},
                        {"Inactive Testcase", "N", "", "", "", "BRS", "Inactive testcase"},
                        {"", "", "openUrl", "", "about:inactive-testcase", "", "Inactive step"}
                }),
                ValidationWorkbookFactory.scenarioSheet("Inactive Flow", new Object[][]{
                        {"Inactive Scenario Testcase", "Y", "", "", "", "BRS", "Inactive scenario"},
                        {"", "", "openUrl", "", "about:inactive-scenario", "", "Inactive step"}
                }),
                ValidationWorkbookFactory.objectRepository(new Object[][]{})
        );
        FakeWebDriver driver = driver();
        AtomicReference<RecordingKeywordEngine> engineReference = new AtomicReference<>();

        try (ExcelReader excelReader = new ExcelReader(inactiveWorkbook.toString())) {
            DataReader dataReader = new DataReader(excelReader);
            ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
            ExcelExecutionConfig executionConfig = executionConfig(inactiveWorkbook, "inactive-report.html");
            ScenarioRunner runner = new ScenarioRunner(
                    new ScenarioReader(excelReader),
                    new StepReader(excelReader),
                    dataReader,
                    objectRepositoryReader,
                    () -> {
                        RecordingKeywordEngine engine = new RecordingKeywordEngine(
                                dataReader,
                                objectRepositoryReader,
                                new KeywordResolver(driver.driver()),
                                executionConfig
                        );
                        engineReference.set(engine);
                        return engine;
                    }
            );

            List<ExecutionResult> results = runner.runActiveScenarios();

            Assert.assertEquals(results.size(), 1);
            Assert.assertEquals(results.get(0).getScenarioNo(), "1");
            Assert.assertEquals(results.get(0).getTestcaseName(), "Active Testcase");
            Assert.assertEquals(driver.getCurrentUrl(), "about:active");
            Assert.assertEquals(engineReference.get().executedSteps.size(), 1);
        }
    }

    private ExcelExecutionConfig executionConfig(Path scenarioFile, String reportName) {
        Properties properties = new Properties();
        properties.setProperty(ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY, scenarioFile.toString());
        properties.setProperty(
                ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY,
                TEMP_DIR.resolve("reports").toString()
        );
        properties.setProperty(ExcelExecutionConfig.REPORT_FILE_NAME_KEY, reportName);
        properties.setProperty(
                ExcelExecutionConfig.SCREENSHOT_OUTPUT_DIRECTORY_KEY,
                TEMP_DIR.resolve("screenshots").toString()
        );
        return ExcelExecutionConfig.fromProperties(properties, Map.of());
    }

    private FakeWebDriver driver() {
        FakeWebDriver driver = new FakeWebDriver();
        driver.setTitle("Excel Keyword Test");
        driver.addElement("//input[@id='username']", "");
        driver.addElement("//input[@id='password']", "");
        driver.addElement("//button[@id='loginButton']", "Login");
        driver.addElement("//h1[@id='dashboard']", "Dashboard");
        driver.addElement("//input[@id='bookingTitle']", "");
        driver.addElement("//button[contains(text(),'Meeting Room A')]", "Meeting Room A");
        driver.addElement("//div[@id='message']", "Booking created successfully");
        return driver;
    }

    private String failures(List<ExecutionResult> results) {
        return results.stream()
                .filter(result -> !result.isSuccess())
                .map(ExecutionResult::getMessage)
                .toList()
                .toString();
    }

    private static class RecordingKeywordEngine extends KeywordEngine {

        private final List<ResolvedStepContext> executedSteps = new ArrayList<>();

        private RecordingKeywordEngine(
                DataReader dataReader,
                ObjectRepositoryReader objectRepositoryReader,
                KeywordResolver keywordResolver,
                ExcelExecutionConfig executionConfig
        ) {
            super(
                    dataReader,
                    objectRepositoryReader,
                    keywordResolver,
                    new ExcelReportConfig(false, true, true),
                    executionConfig
            );
        }

        @Override
        public ExecutionResult execute(ResolvedStepContext step) {
            executedSteps.add(step);
            return super.execute(step);
        }
    }

    private static class CountingDataReader extends DataReader {

        private final AtomicBoolean runtimeStarted;
        private final AtomicInteger resolutionCount;

        private CountingDataReader(
                ExcelReader excelReader,
                AtomicBoolean runtimeStarted,
                AtomicInteger resolutionCount
        ) {
            super(excelReader);
            this.runtimeStarted = runtimeStarted;
            this.resolutionCount = resolutionCount;
        }

        @Override
        public String resolveValue(String rawValue, Scenario scenario) {
            if (runtimeStarted.get()) {
                throw new AssertionError("Runtime attempted to re-resolve a data value.");
            }
            resolutionCount.incrementAndGet();
            return super.resolveValue(rawValue, scenario);
        }
    }

    private static class CountingObjectRepositoryReader extends ObjectRepositoryReader {

        private final AtomicBoolean runtimeStarted;
        private final AtomicInteger resolutionCount;

        private CountingObjectRepositoryReader(
                ExcelReader excelReader,
                DataReader dataReader,
                AtomicBoolean runtimeStarted,
                AtomicInteger resolutionCount
        ) {
            super(excelReader, dataReader);
            this.runtimeStarted = runtimeStarted;
            this.resolutionCount = resolutionCount;
        }

        @Override
        public ResolvedObject resolveObject(TestStep step, Scenario scenario) {
            if (runtimeStarted.get()) {
                throw new AssertionError("Runtime attempted to re-resolve an object or XPath.");
            }
            resolutionCount.incrementAndGet();
            return super.resolveObject(step, scenario);
        }
    }
}
