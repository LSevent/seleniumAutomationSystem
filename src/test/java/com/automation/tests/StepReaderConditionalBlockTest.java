package com.automation.tests;

import com.automation.excel.ExcelReader;
import com.automation.excel.StepReader;
import com.automation.models.FlowDirectiveType;
import com.automation.models.Scenario;
import com.automation.models.TestStep;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.automation.tests.support.ValidationWorkbookFactory.scenarioSheet;

public class StepReaderConditionalBlockTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-17a-conditional-blocks");
    private static final String SHEET = "Conditional Flow";

    @Test
    public void shouldParseConditionalDirectiveRowsInStepOrder() throws IOException {
        Path workbookPath = workbook(
                "valid-conditional-block.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "", "Single meeting branch"},
                        {"", "", "input", "txtMeetingDate", "BOOKING_DATA.MEETING_DATE", "", "Input meeting date"},
                        {"", "", "elseIfEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Repeating Meeting", "", "Repeating branch"},
                        {"", "", "input", "txtStartDate", "BOOKING_DATA.START_DATE", "", "Input start date"},
                        {"", "", "else", "", "", "", "Fallback branch"},
                        {"", "", "screenshot", "", "Unknown schedule type", "", "Capture fallback evidence"},
                        {"", "", "endIf", "", "", "", "End conditional"},
                        {"", "", "click", "btnSubmitBooking", "", "", "Submit booking"}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            List<TestStep> steps = new StepReader(excelReader).getActiveSteps(scenario());

            Assert.assertEquals(steps.size(), 8);
            Assert.assertEquals(steps.get(0).getKeyword(), "ifEquals");
            Assert.assertEquals(steps.get(0).getFlowDirective(), FlowDirectiveType.IF_EQUALS);
            Assert.assertTrue(steps.get(0).isConditionalDirective());
            Assert.assertEquals(steps.get(0).getValue(), "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting");
            Assert.assertEquals(steps.get(2).getFlowDirective(), FlowDirectiveType.ELSE_IF_EQUALS);
            Assert.assertEquals(steps.get(4).getFlowDirective(), FlowDirectiveType.ELSE);
            Assert.assertEquals(steps.get(6).getFlowDirective(), FlowDirectiveType.END_IF);
            Assert.assertEquals(steps.get(7).getKeyword(), "click");
            Assert.assertEquals(steps.get(7).getFlowDirective(), FlowDirectiveType.NONE);
            Assert.assertEquals(steps.get(7).getStepOrder(), 8);
        }
    }

    @Test
    public void nestedConditionalBlocksShouldParse() throws IOException {
        Path workbookPath = workbook(
                "nested-conditional-block.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Repeating Meeting", "", "Outer condition"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.GUEST_TYPE = Internal", "", "Nested condition"},
                        {"", "", "input", "txtGuestQty", "BOOKING_DATA.GUEST_QTY", "", "Input guest qty"},
                        {"", "", "endIf", "", "", "", "End nested condition"},
                        {"", "", "endIf", "", "", "", "End outer condition"}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            List<TestStep> steps = new StepReader(excelReader).getActiveSteps(scenario());

            Assert.assertEquals(steps.size(), 5);
            Assert.assertEquals(steps.get(0).getFlowDirective(), FlowDirectiveType.IF_EQUALS);
            Assert.assertEquals(steps.get(1).getFlowDirective(), FlowDirectiveType.IF_EQUALS);
            Assert.assertEquals(steps.get(3).getFlowDirective(), FlowDirectiveType.END_IF);
            Assert.assertEquals(steps.get(4).getFlowDirective(), FlowDirectiveType.END_IF);
        }
    }

    @Test
    public void elseIfWithoutIfShouldFailClearly() throws IOException {
        Path workbookPath = workbook(
                "elseif-without-if.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "elseIfEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Repeating Meeting", "", "Broken branch"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getActiveSteps(scenario());
            }
        });

        Assert.assertTrue(exception.getMessage().contains(
                "Conditional directive 'elseIfEquals' found without an open 'ifEquals'."
        ));
        Assert.assertTrue(exception.getMessage().contains("Sheet: Conditional Flow."));
        Assert.assertTrue(exception.getMessage().contains("Testcase: Create Booking."));
        Assert.assertTrue(exception.getMessage().contains("Row: 3."));
    }

    @Test
    public void elseIfAfterElseShouldFailClearly() throws IOException {
        Path workbookPath = workbook(
                "elseif-after-else.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "", "Condition"},
                        {"", "", "else", "", "", "", "Fallback"},
                        {"", "", "elseIfEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Repeating Meeting", "", "Invalid branch"},
                        {"", "", "endIf", "", "", "", "End"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getActiveSteps(scenario());
            }
        });

        Assert.assertTrue(exception.getMessage().contains(
                "Conditional directive 'elseIfEquals' cannot appear after 'else' in the same conditional block."
        ));
        Assert.assertTrue(exception.getMessage().contains("Row: 5."));
    }

    @Test
    public void duplicateElseShouldFailClearly() throws IOException {
        Path workbookPath = workbook(
                "duplicate-else.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "", "Condition"},
                        {"", "", "else", "", "", "", "Fallback"},
                        {"", "", "else", "", "", "", "Duplicate fallback"},
                        {"", "", "endIf", "", "", "", "End"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getActiveSteps(scenario());
            }
        });

        Assert.assertTrue(exception.getMessage().contains(
                "Conditional directive 'else' appears more than once in the same conditional block."
        ));
        Assert.assertTrue(exception.getMessage().contains("Row: 5."));
    }

    @Test
    public void endIfWithoutIfShouldFailClearly() throws IOException {
        Path workbookPath = workbook(
                "endif-without-if.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "endIf", "", "", "", "Broken end"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getActiveSteps(scenario());
            }
        });

        Assert.assertTrue(exception.getMessage().contains(
                "Conditional directive 'endIf' found without an open 'ifEquals'."
        ));
        Assert.assertTrue(exception.getMessage().contains("Row: 3."));
    }

    @Test
    public void missingEndIfShouldFailClearly() throws IOException {
        Path workbookPath = workbook(
                "missing-endif.xlsx",
                new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "", "Condition"},
                        {"", "", "input", "txtMeetingDate", "BOOKING_DATA.MEETING_DATE", "", "Input meeting date"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getActiveSteps(scenario());
            }
        });

        Assert.assertTrue(exception.getMessage().contains(
                "Conditional block starting with 'ifEquals' is missing 'endIf'."
        ));
        Assert.assertTrue(exception.getMessage().contains("Row: 3."));
    }

    @Test
    public void inactiveTestcaseConditionalDraftShouldNotBlockParsing() throws IOException {
        Path workbookPath = workbook(
                "inactive-conditional-draft.xlsx",
                new Object[][]{
                        {"Inactive Draft", "N", "", "", "", "", "Inactive testcase"},
                        {"", "", "else", "", "", "", "Broken inactive draft"},
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "click", "btnSubmitBooking", "", "", "Submit booking"}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            List<TestStep> steps = new StepReader(excelReader).getActiveSteps(scenario());

            Assert.assertEquals(steps.size(), 1);
            Assert.assertEquals(steps.get(0).getKeyword(), "click");
        }
    }

    private Path workbook(String fileName, Object[][] rows) throws IOException {
        return ValidationWorkbookFactory.createWorkbook(TEMP_DIR.resolve(fileName), scenarioSheet(SHEET, rows));
    }

    private Scenario scenario() {
        return new Scenario("1", true, SHEET, "Conditional flow test", 2);
    }
}
