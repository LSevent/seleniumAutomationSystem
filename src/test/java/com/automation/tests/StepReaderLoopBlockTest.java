package com.automation.tests;

import com.automation.excel.ExcelReader;
import com.automation.excel.StepReader;
import com.automation.exceptions.FrameworkException;
import com.automation.models.FlowDirectiveType;
import com.automation.models.Scenario;
import com.automation.models.TestStep;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.automation.tests.support.ValidationWorkbookFactory.objectRepository;
import static com.automation.tests.support.ValidationWorkbookFactory.scenarioSheet;
import static com.automation.tests.support.ValidationWorkbookFactory.scenarios;

public class StepReaderLoopBlockTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-18a-loop-blocks");
    private static final Scenario SCENARIO = new Scenario("1", true, "Loop Flow", "Loop flow", 2);

    @Test
    public void validForEachDataRowBlockShouldBeParsed() throws IOException {
        Path workbookPath = workbook(
                "valid-loop-block.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "forEachDataRow", "", "BOOKING_DATA", "", "Start booking loop"},
                        {"", "", "input", "txtBookingTitle", "BOOKING_DATA.BOOKING_TITLE", "", "Input booking title"},
                        {"", "", "endForEachDataRow", "", "", "", "End booking loop"},
                        {"", "", "click", "btnAfterLoop", "", "", "After loop"}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            List<TestStep> steps = new StepReader(excelReader).getActiveSteps(SCENARIO);

            Assert.assertEquals(steps.get(0).getFlowDirective(), FlowDirectiveType.FOR_EACH_DATA_ROW);
            Assert.assertEquals(steps.get(1).getFlowDirective(), FlowDirectiveType.NONE);
            Assert.assertEquals(steps.get(2).getFlowDirective(), FlowDirectiveType.END_FOR_EACH_DATA_ROW);
            Assert.assertEquals(steps.get(3).getFlowDirective(), FlowDirectiveType.NONE);
        }
    }

    @Test
    public void nestedLoopAndConditionBlockShouldBeParsed() throws IOException {
        Path workbookPath = workbook(
                "nested-loop-condition-block.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "forEachDataRow", "", "BOOKING_DATA", "", "Start booking loop"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "", "Single condition"},
                        {"", "", "click", "btnSingleMeeting", "", "", "Single step"},
                        {"", "", "endIf", "", "", "", "End condition"},
                        {"", "", "endForEachDataRow", "", "", "", "End booking loop"}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            List<TestStep> steps = new StepReader(excelReader).getActiveSteps(SCENARIO);

            Assert.assertEquals(steps.get(0).getFlowDirective(), FlowDirectiveType.FOR_EACH_DATA_ROW);
            Assert.assertEquals(steps.get(1).getFlowDirective(), FlowDirectiveType.IF_EQUALS);
            Assert.assertEquals(steps.get(3).getFlowDirective(), FlowDirectiveType.END_IF);
            Assert.assertEquals(steps.get(4).getFlowDirective(), FlowDirectiveType.END_FOR_EACH_DATA_ROW);
        }
    }

    @Test
    public void endForEachDataRowWithoutStartShouldFailClearly() throws IOException {
        Path workbookPath = workbook(
                "end-loop-without-start.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "endForEachDataRow", "", "", "", "Broken loop end"}
                }
        );

        FrameworkException exception = expectStepReaderFailure(workbookPath);

        Assert.assertTrue(exception.getMessage().contains(
                "Loop directive 'endForEachDataRow' found without an open 'forEachDataRow'."
        ));
        Assert.assertTrue(exception.getMessage().contains("Row: 3."));
    }

    @Test
    public void missingEndForEachDataRowShouldFailClearly() throws IOException {
        Path workbookPath = workbook(
                "missing-end-loop.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "forEachDataRow", "", "BOOKING_DATA", "", "Start booking loop"},
                        {"", "", "click", "btnAfterLoop", "", "", "Loop body"}
                }
        );

        FrameworkException exception = expectStepReaderFailure(workbookPath);

        Assert.assertTrue(exception.getMessage().contains(
                "Loop block starting with 'forEachDataRow' is missing 'endForEachDataRow'."
        ));
        Assert.assertTrue(exception.getMessage().contains("Row: 3."));
    }

    @Test
    public void endForEachDataRowCannotCloseIfBlockBeforeEndIf() throws IOException {
        Path workbookPath = workbook(
                "loop-end-crosses-if.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "forEachDataRow", "", "BOOKING_DATA", "", "Start booking loop"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "", "Single condition"},
                        {"", "", "endForEachDataRow", "", "", "", "Invalid loop end"},
                        {"", "", "endIf", "", "", "", "End condition"}
                }
        );

        FrameworkException exception = expectStepReaderFailure(workbookPath);

        Assert.assertTrue(exception.getMessage().contains(
                "Loop directive 'endForEachDataRow' cannot close an 'ifEquals' block before 'endIf'."
        ));
        Assert.assertTrue(exception.getMessage().contains("Row: 5."));
    }

    private FrameworkException expectStepReaderFailure(Path workbookPath) {
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            return Assert.expectThrows(
                    FrameworkException.class,
                    () -> new StepReader(excelReader).getActiveSteps(SCENARIO)
            );
        }
    }

    private Path workbook(String fileName, Object[][] scenarioRows) throws IOException {
        return ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve(fileName),
                scenarios(new Object[][]{{1, "Y", "Loop Flow", "Loop flow"}}),
                scenarioSheet("Loop Flow", scenarioRows),
                objectRepository(new Object[][]{
                        {"BRS", "txtBookingTitle", "//input[@id='bookingTitle']", "Booking title"},
                        {"BRS", "btnSingleMeeting", "//button[@id='single']", "Single"},
                        {"BRS", "btnAfterLoop", "//button[@id='after']", "After loop"}
                })
        );
    }
}
