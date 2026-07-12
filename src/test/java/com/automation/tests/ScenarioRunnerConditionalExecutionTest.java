package com.automation.tests;

import com.automation.config.ExcelExecutionConfig;
import com.automation.engine.KeywordEngine;
import com.automation.engine.KeywordResolver;
import com.automation.engine.ScenarioRunner;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.ExecutionResult;
import com.automation.models.ResolvedStepContext;
import com.automation.reports.ExcelReportConfig;
import com.automation.tests.support.FakeWebDriver;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.automation.tests.support.ValidationWorkbookFactory.objectRepository;
import static com.automation.tests.support.ValidationWorkbookFactory.scenarioSheet;
import static com.automation.tests.support.ValidationWorkbookFactory.scenarios;
import static com.automation.tests.support.ValidationWorkbookFactory.sheet;

public class ScenarioRunnerConditionalExecutionTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-17b-conditional-execution");

    @Test
    public void matchingIfBranchShouldExecuteOnlyIfBranchSteps() throws IOException {
        RunnerResult result = runConditionalWorkbook("single-meeting-branch.xlsx", "Single Meeting");

        Assert.assertEquals(result.executedObjects(), List.of("btnSingleMeeting", "btnAfterConditional"));
        Assert.assertEquals(result.resultForObject("btnSingleMeeting").getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.resultForObject("btnRepeatingMeeting").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForObject("btnFallbackMeeting").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForKeyword("ifEquals").getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.resultForKeyword("elseIfEquals").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForKeyword("else").getStatus(), ExecutionResult.STATUS_SKIP);
    }

    @Test
    public void matchingElseIfBranchShouldExecuteOnlyElseIfBranchSteps() throws IOException {
        RunnerResult result = runConditionalWorkbook("repeating-meeting-branch.xlsx", "Repeating Meeting");

        Assert.assertEquals(result.executedObjects(), List.of("btnRepeatingMeeting", "btnAfterConditional"));
        Assert.assertEquals(result.resultForObject("btnSingleMeeting").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForObject("btnRepeatingMeeting").getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.resultForObject("btnFallbackMeeting").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForKeyword("ifEquals").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForKeyword("elseIfEquals").getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.resultForKeyword("else").getStatus(), ExecutionResult.STATUS_SKIP);
    }

    @Test
    public void elseBranchShouldExecuteWhenNoConditionMatches() throws IOException {
        RunnerResult result = runConditionalWorkbook("fallback-branch.xlsx", "Ad Hoc Meeting");

        Assert.assertEquals(result.executedObjects(), List.of("btnFallbackMeeting", "btnAfterConditional"));
        Assert.assertEquals(result.resultForObject("btnSingleMeeting").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForObject("btnRepeatingMeeting").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForObject("btnFallbackMeeting").getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.resultForKeyword("else").getStatus(), ExecutionResult.STATUS_PASS);
    }

    @Test
    public void conditionComparisonShouldTrimAndIgnoreCase() throws IOException {
        RunnerResult result = runConditionalWorkbook("case-insensitive-condition.xlsx", "single meeting");

        Assert.assertEquals(result.executedObjects(), List.of("btnSingleMeeting", "btnAfterConditional"));
        Assert.assertEquals(result.resultForObject("btnSingleMeeting").getStatus(), ExecutionResult.STATUS_PASS);
    }

    @Test
    public void directiveRowsShouldNotBeSentToKeywordEngine() throws IOException {
        RunnerResult result = runConditionalWorkbook("directives-not-executed.xlsx", "Single Meeting");

        Assert.assertFalse(result.executedKeywords().contains("ifEquals"));
        Assert.assertFalse(result.executedKeywords().contains("elseIfEquals"));
        Assert.assertFalse(result.executedKeywords().contains("else"));
        Assert.assertFalse(result.executedKeywords().contains("endIf"));
    }

    @Test
    public void displayedIfBranchShouldExecuteWhenObjectIsVisible() throws IOException {
        RunnerResult result = runDisplayedConditionalWorkbook("displayed-if-branch.xlsx", true);

        Assert.assertEquals(result.executedObjects(), List.of("btnCloseWarning", "btnAfterConditional"));
        Assert.assertEquals(result.resultForKeyword("ifDisplayed").getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.resultForKeyword("else").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForObject("btnCloseWarning").getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.resultForObject("btnFallback").getStatus(), ExecutionResult.STATUS_SKIP);
    }

    @Test
    public void elseBranchShouldExecuteWhenDisplayedObjectIsMissing() throws IOException {
        RunnerResult result = runDisplayedConditionalWorkbook("displayed-else-branch.xlsx", false);

        Assert.assertEquals(result.executedObjects(), List.of("btnFallback", "btnAfterConditional"));
        Assert.assertEquals(result.resultForKeyword("ifDisplayed").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForKeyword("else").getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.resultForObject("btnCloseWarning").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForObject("btnFallback").getStatus(), ExecutionResult.STATUS_PASS);
    }

    @Test
    public void outerElseShouldExecuteWhenNestedConditionalIsInsideSkippedIfBranch() throws IOException {
        RunnerResult result = runNestedConditionalWorkbook("nested-outer-else.xlsx", "Repeating Meeting");

        Assert.assertEquals(result.executedObjects(), List.of("btnOuterFallback", "btnAfterConditional"));
        Assert.assertEquals(result.resultForObject("btnOuterTrue").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForObject("btnInnerTrue").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForObject("btnInnerFallback").getStatus(), ExecutionResult.STATUS_SKIP);
        Assert.assertEquals(result.resultForObject("btnOuterFallback").getStatus(), ExecutionResult.STATUS_PASS);
    }

    private RunnerResult runConditionalWorkbook(String fileName, String scheduleType) throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve(fileName),
                scenarios(new Object[][]{{1, "Y", "Conditional Flow", "Conditional execution"}}),
                scenarioSheet("Conditional Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE", "", "Single Meeting"},
                        {"", "", "click", "btnSingleMeeting", "", "", "Single meeting step"},
                        {"", "", "elseIfEquals", "", "BOOKING_DATA.SCHEDULE_TYPE", "", "Repeating Meeting"},
                        {"", "", "click", "btnRepeatingMeeting", "", "", "Repeating meeting step"},
                        {"", "", "else", "", "", "", "Fallback condition"},
                        {"", "", "click", "btnFallbackMeeting", "", "", "Fallback step"},
                        {"", "", "endIf", "", "", "", "End condition"},
                        {"", "", "click", "btnAfterConditional", "", "", "After conditional step"}
                }),
                sheet("BOOKING_DATA", new String[]{"NO", "SCHEDULE_TYPE"}, new Object[][]{{1, scheduleType}}),
                objectRepository(new Object[][]{
                        {"BRS", "btnSingleMeeting", "//button[@id='single']", "Single"},
                        {"BRS", "btnRepeatingMeeting", "//button[@id='repeating']", "Repeating"},
                        {"BRS", "btnFallbackMeeting", "//button[@id='fallback']", "Fallback"},
                        {"BRS", "btnAfterConditional", "//button[@id='after']", "After"}
                })
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            DataReader dataReader = new DataReader(excelReader);
            ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
            FakeWebDriver driver = new FakeWebDriver();
            RecordingKeywordEngine keywordEngine = new RecordingKeywordEngine(
                    dataReader,
                    objectRepositoryReader,
                    new KeywordResolver(driver.driver()),
                    executionConfig(workbookPath)
            );
            ScenarioRunner runner = new ScenarioRunner(
                    new ScenarioReader(excelReader),
                    new StepReader(excelReader),
                    dataReader,
                    objectRepositoryReader,
                    () -> keywordEngine
            );

            return new RunnerResult(runner.runActiveScenarios(), keywordEngine.executedSteps);
        }
    }

    private RunnerResult runDisplayedConditionalWorkbook(String fileName, boolean warningVisible) throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve(fileName),
                scenarios(new Object[][]{{1, "Y", "Displayed Conditional Flow", "Displayed conditional execution"}}),
                scenarioSheet("Displayed Conditional Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifDisplayed", "dlgWarning", "", "", "Warning exists branch"},
                        {"", "", "click", "btnCloseWarning", "", "", "Close warning"},
                        {"", "", "else", "", "", "", "Fallback condition"},
                        {"", "", "click", "btnFallback", "", "", "Fallback step"},
                        {"", "", "endIf", "", "", "", "End condition"},
                        {"", "", "click", "btnAfterConditional", "", "", "After conditional step"}
                }),
                objectRepository(new Object[][]{
                        {"BRS", "dlgWarning", "//div[@id='warning']", "Warning"},
                        {"BRS", "btnCloseWarning", "//button[@id='close-warning']", "Close"},
                        {"BRS", "btnFallback", "//button[@id='fallback']", "Fallback"},
                        {"BRS", "btnAfterConditional", "//button[@id='after']", "After"}
                })
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            DataReader dataReader = new DataReader(excelReader);
            ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
            FakeWebDriver driver = new FakeWebDriver();
            if (warningVisible) {
                driver.addElement("//div[@id='warning']", "Warning");
            }
            RecordingKeywordEngine keywordEngine = new RecordingKeywordEngine(
                    dataReader,
                    objectRepositoryReader,
                    new KeywordResolver(driver.driver()),
                    executionConfig(workbookPath)
            );
            ScenarioRunner runner = new ScenarioRunner(
                    new ScenarioReader(excelReader),
                    new StepReader(excelReader),
                    dataReader,
                    objectRepositoryReader,
                    () -> keywordEngine
            );

            return new RunnerResult(runner.runActiveScenarios(), keywordEngine.executedSteps);
        }
    }

    private RunnerResult runNestedConditionalWorkbook(String fileName, String scheduleType) throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve(fileName),
                scenarios(new Object[][]{{1, "Y", "Nested Conditional Flow", "Nested conditional execution"}}),
                scenarioSheet("Nested Conditional Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE", "", "Single Meeting"},
                        {"", "", "click", "btnOuterTrue", "", "", "Outer true step"},
                        {"", "", "ifDisplayed", "dlgWarning", "", "", "Inner displayed condition"},
                        {"", "", "click", "btnInnerTrue", "", "", "Inner true step"},
                        {"", "", "else", "", "", "", "Inner fallback condition"},
                        {"", "", "click", "btnInnerFallback", "", "", "Inner fallback step"},
                        {"", "", "endIf", "", "", "", "End inner condition"},
                        {"", "", "else", "", "", "", "Outer fallback condition"},
                        {"", "", "click", "btnOuterFallback", "", "", "Outer fallback step"},
                        {"", "", "endIf", "", "", "", "End outer condition"},
                        {"", "", "click", "btnAfterConditional", "", "", "After conditional step"}
                }),
                sheet("BOOKING_DATA", new String[]{"NO", "SCHEDULE_TYPE"}, new Object[][]{{1, scheduleType}}),
                objectRepository(new Object[][]{
                        {"BRS", "dlgWarning", "//div[@id='warning']", "Warning"},
                        {"BRS", "btnOuterTrue", "//button[@id='outer-true']", "Outer true"},
                        {"BRS", "btnInnerTrue", "//button[@id='inner-true']", "Inner true"},
                        {"BRS", "btnInnerFallback", "//button[@id='inner-fallback']", "Inner fallback"},
                        {"BRS", "btnOuterFallback", "//button[@id='outer-fallback']", "Outer fallback"},
                        {"BRS", "btnAfterConditional", "//button[@id='after']", "After"}
                })
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            DataReader dataReader = new DataReader(excelReader);
            ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
            FakeWebDriver driver = new FakeWebDriver();
            driver.addElement("//div[@id='warning']", "Warning");
            RecordingKeywordEngine keywordEngine = new RecordingKeywordEngine(
                    dataReader,
                    objectRepositoryReader,
                    new KeywordResolver(driver.driver()),
                    executionConfig(workbookPath)
            );
            ScenarioRunner runner = new ScenarioRunner(
                    new ScenarioReader(excelReader),
                    new StepReader(excelReader),
                    dataReader,
                    objectRepositoryReader,
                    () -> keywordEngine
            );

            return new RunnerResult(runner.runActiveScenarios(), keywordEngine.executedSteps);
        }
    }

    private ExcelExecutionConfig executionConfig(Path scenarioFile) {
        Properties properties = new Properties();
        properties.setProperty(ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY, scenarioFile.toString());
        properties.setProperty(
                ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY,
                TEMP_DIR.resolve("reports").toString()
        );
        properties.setProperty(ExcelExecutionConfig.CONDITION_TIME_KEY, "1");
        return ExcelExecutionConfig.fromProperties(properties, Map.of());
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
            return ExecutionResult.success(
                    step,
                    RecordingKeywordEngine.class.getName(),
                    "TEST",
                    "Recorded execution."
            );
        }
    }

    private record RunnerResult(
            List<ExecutionResult> results,
            List<ResolvedStepContext> executedSteps
    ) {
        private List<String> executedObjects() {
            return executedSteps.stream()
                    .map(ResolvedStepContext::getObjectName)
                    .toList();
        }

        private List<String> executedKeywords() {
            return executedSteps.stream()
                    .map(ResolvedStepContext::getKeyword)
                    .toList();
        }

        private ExecutionResult resultForObject(String objectName) {
            return results.stream()
                    .filter(result -> objectName.equals(result.getObjectName()))
                    .findFirst()
                    .orElseThrow();
        }

        private ExecutionResult resultForKeyword(String keyword) {
            return results.stream()
                    .filter(result -> keyword.equals(result.getKeywordName()))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
