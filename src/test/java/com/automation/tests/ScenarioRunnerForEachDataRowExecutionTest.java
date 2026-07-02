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

public class ScenarioRunnerForEachDataRowExecutionTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-18a-loop-execution");

    @Test
    public void forEachDataRowShouldExecuteBodyForEachMatchingDataRow() throws IOException {
        RunnerResult result = runLoopWorkbook("loop-execution.xlsx", "BOOKING_DATA");

        Assert.assertEquals(
                result.executedObjects(),
                List.of(
                        "txtBookingTitle",
                        "btnRoomByName",
                        "btnSingleMeeting",
                        "txtBookingTitle",
                        "btnRoomByName",
                        "btnRepeatingMeeting",
                        "btnAfterLoop"
                )
        );
        Assert.assertEquals(result.resolvedValuesForObject("txtBookingTitle"), List.of("Weekly Meeting", "Daily Standup"));
        Assert.assertEquals(
                result.resolvedXPathsForObject("btnRoomByName"),
                List.of(
                        "//button[contains(text(),'Meeting Room A')]",
                        "//button[contains(text(),'Meeting Room B')]"
                )
        );
        Assert.assertEquals(result.resultCountForKeyword("forEachDataRow"), 2);
        Assert.assertEquals(result.resultCountForKeyword("endForEachDataRow"), 2);
        Assert.assertFalse(result.executedKeywords().contains("forEachDataRow"));
        Assert.assertFalse(result.executedKeywords().contains("endForEachDataRow"));
    }

    @Test
    public void forEachDataRowShouldSupportHashPrefixedSheetName() throws IOException {
        RunnerResult result = runLoopWorkbook("hash-prefixed-loop.xlsx", "#BOOKING_DATA");

        Assert.assertEquals(result.resolvedValuesForObject("txtBookingTitle"), List.of("Weekly Meeting", "Daily Standup"));
        Assert.assertEquals(result.resultCountForKeyword("forEachDataRow"), 2);
    }

    @Test
    public void conditionalBlocksInsideLoopShouldUseCurrentDataRow() throws IOException {
        RunnerResult result = runLoopWorkbook("loop-with-conditionals.xlsx", "BOOKING_DATA");

        Assert.assertEquals(result.resultCountForObjectAndStatus("btnSingleMeeting", ExecutionResult.STATUS_PASS), 1);
        Assert.assertEquals(result.resultCountForObjectAndStatus("btnSingleMeeting", ExecutionResult.STATUS_SKIP), 1);
        Assert.assertEquals(result.resultCountForObjectAndStatus("btnRepeatingMeeting", ExecutionResult.STATUS_PASS), 1);
        Assert.assertEquals(result.resultCountForObjectAndStatus("btnRepeatingMeeting", ExecutionResult.STATUS_SKIP), 1);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("ifEquals", ExecutionResult.STATUS_PASS), 1);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("ifEquals", ExecutionResult.STATUS_SKIP), 1);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("elseIfEquals", ExecutionResult.STATUS_PASS), 1);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("elseIfEquals", ExecutionResult.STATUS_SKIP), 1);
    }

    @Test
    public void conditionWrappingLoopShouldExecuteLoopWhenConditionMatches() throws IOException {
        RunnerResult result = runConditionWrappingLoopWorkbook("condition-wraps-matching-loop.xlsx", "Yes");

        Assert.assertEquals(result.executedObjects(), List.of("txtBookingTitle", "txtBookingTitle", "btnAfterFlow"));
        Assert.assertEquals(result.resolvedValuesForObject("txtBookingTitle"), List.of("Weekly Meeting", "Daily Standup"));
        Assert.assertEquals(result.resultCountForKeywordAndStatus("ifEquals", ExecutionResult.STATUS_PASS), 1);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("forEachDataRow", ExecutionResult.STATUS_PASS), 2);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("endForEachDataRow", ExecutionResult.STATUS_PASS), 2);
        Assert.assertEquals(result.resultCountForObjectAndStatus("btnSkippedLoopFallback", ExecutionResult.STATUS_SKIP), 1);
    }

    @Test
    public void conditionWrappingLoopShouldSkipLoopWhenConditionDoesNotMatch() throws IOException {
        RunnerResult result = runConditionWrappingLoopWorkbook("condition-wraps-skipped-loop.xlsx", "No");

        Assert.assertEquals(result.executedObjects(), List.of("btnSkippedLoopFallback", "btnAfterFlow"));
        Assert.assertEquals(result.resultCountForObjectAndStatus("txtBookingTitle", ExecutionResult.STATUS_SKIP), 2);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("ifEquals", ExecutionResult.STATUS_SKIP), 1);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("forEachDataRow", ExecutionResult.STATUS_SKIP), 2);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("endForEachDataRow", ExecutionResult.STATUS_SKIP), 2);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("else", ExecutionResult.STATUS_PASS), 1);
    }

    @Test
    public void loopInsideElseBranchShouldExecuteWhenElseBranchIsActive() throws IOException {
        RunnerResult result = runLoopInsideElseWorkbook("loop-inside-else-branch.xlsx");

        Assert.assertEquals(result.executedObjects(), List.of("txtBookingTitle", "txtBookingTitle", "btnAfterFlow"));
        Assert.assertEquals(result.resolvedValuesForObject("txtBookingTitle"), List.of("Weekly Meeting", "Daily Standup"));
        Assert.assertEquals(result.resultCountForObjectAndStatus("btnIfBranch", ExecutionResult.STATUS_SKIP), 1);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("else", ExecutionResult.STATUS_PASS), 1);
        Assert.assertEquals(result.resultCountForKeywordAndStatus("forEachDataRow", ExecutionResult.STATUS_PASS), 2);
    }

    private RunnerResult runLoopWorkbook(String fileName, String loopValue) throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve(fileName),
                scenarios(new Object[][]{{1, "Y", "Loop Flow", "Loop execution"}}),
                scenarioSheet("Loop Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "forEachDataRow", "", loopValue, "", "Start booking-data loop"},
                        {"", "", "input", "txtBookingTitle", "BOOKING_DATA.BOOKING_TITLE", "", "Input booking title"},
                        {"", "", "click", "btnRoomByName", "BOOKING_DATA.ROOM_NAME", "", "Select room"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "", "Single condition"},
                        {"", "", "click", "btnSingleMeeting", "", "", "Single meeting step"},
                        {"", "", "elseIfEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Repeating Meeting", "", "Repeating condition"},
                        {"", "", "click", "btnRepeatingMeeting", "", "", "Repeating meeting step"},
                        {"", "", "endIf", "", "", "", "End condition"},
                        {"", "", "endForEachDataRow", "", "", "", "End booking-data loop"},
                        {"", "", "click", "btnAfterLoop", "", "", "After loop"}
                }),
                sheet("BOOKING_DATA", new String[]{"NO", "SCHEDULE_TYPE", "BOOKING_TITLE", "ROOM_NAME"}, new Object[][]{
                        {1, "Single Meeting", "Weekly Meeting", "Meeting Room A"},
                        {1, "Repeating Meeting", "Daily Standup", "Meeting Room B"},
                        {2, "Single Meeting", "Other Scenario Meeting", "Meeting Room C"}
                }),
                objectRepository(new Object[][]{
                        {"BRS", "txtBookingTitle", "//input[@id='bookingTitle']", "Booking title"},
                        {"BRS", "btnRoomByName", "//button[contains(text(),'{ROOM_NAME}')]", "Dynamic room"},
                        {"BRS", "btnSingleMeeting", "//button[@id='single']", "Single"},
                        {"BRS", "btnRepeatingMeeting", "//button[@id='repeating']", "Repeating"},
                        {"BRS", "btnAfterLoop", "//button[@id='after-loop']", "After loop"}
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

    private RunnerResult runConditionWrappingLoopWorkbook(String fileName, String runBookingsValue) throws IOException {
        return runWorkbook(
                fileName,
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "CONFIG.RUN_BOOKINGS = Yes", "", "Run booking loop condition"},
                        {"", "", "forEachDataRow", "", "BOOKING_DATA", "", "Start booking-data loop"},
                        {"", "", "input", "txtBookingTitle", "BOOKING_DATA.BOOKING_TITLE", "", "Input booking title"},
                        {"", "", "endForEachDataRow", "", "", "", "End booking-data loop"},
                        {"", "", "else", "", "", "", "Loop skipped fallback"},
                        {"", "", "click", "btnSkippedLoopFallback", "", "", "Fallback when loop skipped"},
                        {"", "", "endIf", "", "", "", "End condition"},
                        {"", "", "click", "btnAfterFlow", "", "", "After conditional loop"}
                },
                runBookingsValue
        );
    }

    private RunnerResult runLoopInsideElseWorkbook(String fileName) throws IOException {
        return runWorkbook(
                fileName,
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "CONFIG.RUN_BOOKINGS = No", "", "Inactive if branch"},
                        {"", "", "click", "btnIfBranch", "", "", "Should be skipped"},
                        {"", "", "else", "", "", "", "Loop else branch"},
                        {"", "", "forEachDataRow", "", "BOOKING_DATA", "", "Start booking-data loop"},
                        {"", "", "input", "txtBookingTitle", "BOOKING_DATA.BOOKING_TITLE", "", "Input booking title"},
                        {"", "", "endForEachDataRow", "", "", "", "End booking-data loop"},
                        {"", "", "endIf", "", "", "", "End condition"},
                        {"", "", "click", "btnAfterFlow", "", "", "After conditional loop"}
                },
                "Yes"
        );
    }

    private RunnerResult runWorkbook(
            String fileName,
            Object[][] scenarioRows,
            String runBookingsValue
    ) throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve(fileName),
                scenarios(new Object[][]{{1, "Y", "Loop Flow", "Loop execution"}}),
                scenarioSheet("Loop Flow", scenarioRows),
                sheet("CONFIG", new String[]{"NO", "RUN_BOOKINGS"}, new Object[][]{
                        {1, runBookingsValue}
                }),
                sheet("BOOKING_DATA", new String[]{"NO", "BOOKING_TITLE"}, new Object[][]{
                        {1, "Weekly Meeting"},
                        {1, "Daily Standup"},
                        {2, "Other Scenario Meeting"}
                }),
                objectRepository(new Object[][]{
                        {"BRS", "txtBookingTitle", "//input[@id='bookingTitle']", "Booking title"},
                        {"BRS", "btnIfBranch", "//button[@id='if-branch']", "If branch"},
                        {"BRS", "btnSkippedLoopFallback", "//button[@id='fallback']", "Loop skipped fallback"},
                        {"BRS", "btnAfterFlow", "//button[@id='after-flow']", "After flow"}
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

    private ExcelExecutionConfig executionConfig(Path scenarioFile) {
        Properties properties = new Properties();
        properties.setProperty(ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY, scenarioFile.toString());
        properties.setProperty(
                ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY,
                TEMP_DIR.resolve("reports").toString()
        );
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

        private List<String> resolvedValuesForObject(String objectName) {
            return executedSteps.stream()
                    .filter(step -> objectName.equals(step.getObjectName()))
                    .map(ResolvedStepContext::getResolvedValue)
                    .toList();
        }

        private List<String> resolvedXPathsForObject(String objectName) {
            return executedSteps.stream()
                    .filter(step -> objectName.equals(step.getObjectName()))
                    .map(ResolvedStepContext::getResolvedXPath)
                    .toList();
        }

        private long resultCountForKeyword(String keyword) {
            return results.stream()
                    .filter(result -> keyword.equals(result.getKeywordName()))
                    .count();
        }

        private long resultCountForObjectAndStatus(String objectName, String status) {
            return results.stream()
                    .filter(result -> objectName.equals(result.getObjectName()))
                    .filter(result -> status.equals(result.getStatus()))
                    .count();
        }

        private long resultCountForKeywordAndStatus(String keyword, String status) {
            return results.stream()
                    .filter(result -> keyword.equals(result.getKeywordName()))
                    .filter(result -> status.equals(result.getStatus()))
                    .count();
        }
    }
}
