package com.automation.tests;

import com.automation.engine.ExecutionPlanBuilder;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.exceptions.FrameworkException;
import com.automation.models.FlowDirectiveType;
import com.automation.models.ResolvedScenarioContext;
import com.automation.models.ResolvedStepContext;
import com.automation.models.ResolvedTestcaseContext;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.automation.tests.support.ValidationWorkbookFactory.objectRepository;
import static com.automation.tests.support.ValidationWorkbookFactory.formula;
import static com.automation.tests.support.ValidationWorkbookFactory.scenarioSheet;
import static com.automation.tests.support.ValidationWorkbookFactory.scenarios;
import static com.automation.tests.support.ValidationWorkbookFactory.sheet;

public class ExecutionPlanBuilderTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-13b-plan-builder");

    @Test
    public void shouldBuildResolvedPlanForActiveScenariosAndTestcases() throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("resolved-plan.xlsx"),
                scenarios(new Object[][]{
                        {1, "Y", "Booking Flow", "Create booking"},
                        {2, "N", "Missing Inactive Sheet", "Ignored inactive scenario"}
                }),
                scenarioSheet("Booking Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "input", "txtBookingTitle", "BOOKING_DATA.BOOKING_TITLE", "", "Input title"},
                        {"", "", "click", "btnRoomByName", "BOOKING_DATA.ROOM_NAME", "", "Choose room"},
                        {"Inactive Broken Testcase", "N", "", "", "", "", "Ignored testcase"},
                        {"", "", "input", "missingObject", "MISSING_DATA.VALUE", "", "Ignored step"}
                }),
                sheet("BOOKING_DATA", new String[]{"ROOM_NAME", "NO", "BOOKING_TITLE"}, new Object[][]{
                        {"Meeting Room A", 1, "Weekly Meeting"}
                }),
                objectRepository(new Object[][]{
                        {"BRS", "txtBookingTitle", "//input[@id='bookingTitle']", "Title"},
                        {"BRS", "btnRoomByName", "//button[contains(text(),'{ROOM_NAME}')]", "Room"},
                        {"", "unusedBrokenObject", "", "Unused row must not block active resolution"}
                })
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            List<ResolvedScenarioContext> plan = builder(excelReader).build();

            Assert.assertEquals(plan.size(), 1);
            ResolvedScenarioContext scenario = plan.get(0);
            Assert.assertEquals(scenario.getScenarioNo(), "1");
            Assert.assertEquals(scenario.getScenarioAction(), "Booking Flow");
            Assert.assertEquals(scenario.getScenarioName(), "Create booking");
            Assert.assertEquals(scenario.getTestcases().size(), 1);

            ResolvedTestcaseContext testcase = scenario.getTestcases().get(0);
            Assert.assertEquals(testcase.getTestcaseName(), "Create Booking");
            Assert.assertEquals(testcase.getApplication(), "BRS");
            Assert.assertEquals(testcase.getParentExcelRow(), 2);
            Assert.assertEquals(testcase.getSteps().size(), 2);

            ResolvedStepContext inputStep = testcase.getSteps().get(0);
            Assert.assertEquals(inputStep.getSheetName(), "Booking Flow");
            Assert.assertEquals(inputStep.getTestcaseName(), "Create Booking");
            Assert.assertEquals(inputStep.getTestcaseParentRow(), 2);
            Assert.assertEquals(inputStep.getExcelRow(), 3);
            Assert.assertEquals(inputStep.getStepNumber(), 1);
            Assert.assertEquals(inputStep.getApplication(), "BRS");
            Assert.assertEquals(inputStep.getRawValue(), "BOOKING_DATA.BOOKING_TITLE");
            Assert.assertEquals(inputStep.getResolvedValue(), "Weekly Meeting");
            Assert.assertEquals(inputStep.getRawXPath(), "//input[@id='bookingTitle']");
            Assert.assertEquals(inputStep.getResolvedXPath(), "//input[@id='bookingTitle']");
            Assert.assertEquals(inputStep.getExecutedBy(), "");

            ResolvedStepContext dynamicStep = testcase.getSteps().get(1);
            Assert.assertEquals(dynamicStep.getApplication(), "BRS");
            Assert.assertEquals(dynamicStep.getRawValue(), "BOOKING_DATA.ROOM_NAME");
            Assert.assertEquals(dynamicStep.getResolvedValue(), "Meeting Room A");
            Assert.assertEquals(dynamicStep.getRawXPath(), "//button[contains(text(),'{ROOM_NAME}')]");
            Assert.assertEquals(dynamicStep.getResolvedXPath(), "//button[contains(text(),'Meeting Room A')]");
        }
    }

    @Test
    public void mismatchedDynamicXPathPlaceholderShouldFailWithStepContext() throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("mismatched-placeholder.xlsx"),
                scenarios(new Object[][]{{1, "Y", "Booking Flow", "Create booking"}}),
                scenarioSheet("Booking Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "click", "btnRoomByName", "BOOKING_DATA.ROOM_NAME", "", "Choose room"}
                }),
                sheet("BOOKING_DATA", new String[]{"NO", "ROOM_NAME"}, new Object[][]{{1, "Meeting Room A"}}),
                objectRepository(new Object[][]{
                        {"BRS", "btnRoomByName", "//button[contains(text(),'{OTHER_NAME}')]", "Room"}
                })
        );

        FrameworkException exception = Assert.expectThrows(FrameworkException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                builder(excelReader).build();
            }
        });

        Assert.assertTrue(exception.getMessage().contains("XPath placeholder {OTHER_NAME} does not match data column 'ROOM_NAME'"));
        Assert.assertTrue(exception.getMessage().contains("Scenario NO: 1."));
        Assert.assertTrue(exception.getMessage().contains("Testcase: Create Booking."));
        Assert.assertTrue(exception.getMessage().contains("Row: 3."));
        Assert.assertTrue(exception.getMessage().contains("Keyword: click."));
        Assert.assertTrue(exception.getMessage().contains("Object: btnRoomByName."));
        Assert.assertTrue(exception.getMessage().contains("Application: BRS."));
    }

    @Test
    public void conditionalDirectiveRowsShouldBePreservedWithoutObjectOrValueResolution() throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("conditional-directives-preserved.xlsx"),
                scenarios(new Object[][]{{1, "Y", "Booking Flow", "Create booking"}}),
                scenarioSheet("Booking Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting", "", "Single meeting condition"},
                        {"", "", "click", "btnSubmitBooking", "", "", "Submit booking"},
                        {"", "", "endIf", "", "", "", "End condition"}
                }),
                sheet("BOOKING_DATA", new String[]{"NO", "SCHEDULE_TYPE"}, new Object[][]{{1, "Single Meeting"}}),
                objectRepository(new Object[][]{
                        {"BRS", "btnSubmitBooking", "//button[@id='submitBooking']", "Submit"}
                })
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ResolvedTestcaseContext testcase = builder(excelReader)
                    .build()
                    .get(0)
                    .getTestcases()
                    .get(0);

            Assert.assertEquals(testcase.getSteps().size(), 3);

            ResolvedStepContext ifStep = testcase.getSteps().get(0);
            Assert.assertEquals(ifStep.getKeyword(), "ifEquals");
            Assert.assertEquals(ifStep.getFlowDirective(), FlowDirectiveType.IF_EQUALS);
            Assert.assertEquals(ifStep.getRawValue(), "BOOKING_DATA.SCHEDULE_TYPE = Single Meeting");
            Assert.assertEquals(ifStep.getResolvedValue(), "Single Meeting = Single Meeting");
            Assert.assertEquals(ifStep.getRawXPath(), "");
            Assert.assertEquals(ifStep.getResolvedXPath(), "");

            ResolvedStepContext clickStep = testcase.getSteps().get(1);
            Assert.assertEquals(clickStep.getKeyword(), "click");
            Assert.assertEquals(clickStep.getFlowDirective(), FlowDirectiveType.NONE);
            Assert.assertEquals(clickStep.getResolvedXPath(), "//button[@id='submitBooking']");

            ResolvedStepContext endStep = testcase.getSteps().get(2);
            Assert.assertEquals(endStep.getFlowDirective(), FlowDirectiveType.END_IF);
        }
    }

    @Test
    public void conditionalDirectiveShouldUseDescriptionAsExpectedValueWhenValueContainsActualOnly() throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("conditional-description-expected-value.xlsx"),
                scenarios(new Object[][]{{1, "Y", "Booking Flow", "Create booking"}}),
                scenarioSheet("Booking Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", formula("BOOKING_DATA!$B$1"), "", "Single Meeting"},
                        {"", "", "click", "btnSubmitBooking", "", "", "Submit booking"},
                        {"", "", "endIf", "", "", "", "End condition"}
                }),
                sheet("BOOKING_DATA", new String[]{"NO", "SCHEDULE_TYPE"}, new Object[][]{{1, "Single Meeting"}}),
                objectRepository(new Object[][]{
                        {"BRS", "btnSubmitBooking", "//button[@id='submitBooking']", "Submit"}
                })
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ResolvedStepContext ifStep = builder(excelReader)
                    .build()
                    .get(0)
                    .getTestcases()
                    .get(0)
                    .getSteps()
                    .get(0);

            Assert.assertEquals(ifStep.getRawValue(), "=BOOKING_DATA!$B$1");
            Assert.assertEquals(ifStep.getResolvedValue(), "Single Meeting = Single Meeting");
            Assert.assertEquals(ifStep.getDescription(), "Single Meeting");
        }
    }

    @Test
    public void conditionalDirectiveShouldSupportFormulaReferenceInInlineCondition() throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("conditional-inline-formula-reference.xlsx"),
                scenarios(new Object[][]{{1, "Y", "Booking Flow", "Create booking"}}),
                scenarioSheet("Booking Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "ifEquals", "", "=BOOKING_DATA!$B$1 = Single Meeting", "", "Single meeting condition"},
                        {"", "", "click", "btnSubmitBooking", "", "", "Submit booking"},
                        {"", "", "endIf", "", "", "", "End condition"}
                }),
                sheet("BOOKING_DATA", new String[]{"NO", "SCHEDULE_TYPE"}, new Object[][]{{1, "Single Meeting"}}),
                objectRepository(new Object[][]{
                        {"BRS", "btnSubmitBooking", "//button[@id='submitBooking']", "Submit"}
                })
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ResolvedStepContext ifStep = builder(excelReader)
                    .build()
                    .get(0)
                    .getTestcases()
                    .get(0)
                    .getSteps()
                    .get(0);

            Assert.assertEquals(ifStep.getRawValue(), "=BOOKING_DATA!$B$1 = Single Meeting");
            Assert.assertEquals(ifStep.getResolvedValue(), "Single Meeting = Single Meeting");
        }
    }

    @Test
    public void formulaHeaderReferenceShouldResolveUsingCurrentScenarioDataRow() throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("formula-header-reference.xlsx"),
                scenarios(new Object[][]{{1, "Y", "Booking Flow", "Create booking"}}),
                scenarioSheet("Booking Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "click", "btnBookingByTitle", formula("BOOKING_DATA!$B$1"), "", "Click title from formula header"}
                }),
                sheet("BOOKING_DATA", new String[]{"NO", "BOOKING_TITLE"}, new Object[][]{
                        {1, "Formula Header Meeting"}
                }),
                objectRepository(new Object[][]{
                        {"BRS", "btnBookingByTitle", "//button[text()='{BOOKING_TITLE}']", "Title button"}
                })
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ResolvedStepContext step = builder(excelReader)
                    .build()
                    .get(0)
                    .getTestcases()
                    .get(0)
                    .getSteps()
                    .get(0);

            Assert.assertEquals(step.getRawValue(), "=BOOKING_DATA!$B$1");
            Assert.assertEquals(step.getResolvedValue(), "Formula Header Meeting");
            Assert.assertEquals(step.getRawXPath(), "//button[text()='{BOOKING_TITLE}']");
            Assert.assertEquals(step.getResolvedXPath(), "//button[text()='Formula Header Meeting']");
        }
    }

    @Test
    public void formulaHeaderReferenceInsideLoopShouldUseCurrentLoopRow() throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("formula-header-reference-loop.xlsx"),
                scenarios(new Object[][]{{1, "Y", "Booking Flow", "Create booking"}}),
                scenarioSheet("Booking Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "forEachDataRow", "", "#BOOKING_DATA", "", "Loop bookings"},
                        {"", "", "input", "txtBookingTitle", formula("BOOKING_DATA!$B$1"), "", "Input loop title"},
                        {"", "", "endForEachDataRow", "", "", "", "End loop"}
                }),
                sheet("BOOKING_DATA", new String[]{"NO", "BOOKING_TITLE"}, new Object[][]{
                        {1, "First Meeting"},
                        {1, "Second Meeting"}
                }),
                objectRepository(new Object[][]{
                        {"BRS", "txtBookingTitle", "//input[@id='bookingTitle']", "Title"}
                })
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            List<ResolvedStepContext> steps = builder(excelReader)
                    .build()
                    .get(0)
                    .getTestcases()
                    .get(0)
                    .getSteps();

            Assert.assertEquals(steps.size(), 6);
            Assert.assertEquals(steps.get(1).getRawValue(), "=BOOKING_DATA!$B$1");
            Assert.assertEquals(steps.get(1).getResolvedValue(), "First Meeting");
            Assert.assertEquals(steps.get(4).getRawValue(), "=BOOKING_DATA!$B$1");
            Assert.assertEquals(steps.get(4).getResolvedValue(), "Second Meeting");
        }
    }

    @Test
    public void ordinaryFormulaValueShouldStillEvaluateAsRawValue() throws IOException {
        Path workbookPath = ValidationWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("ordinary-formula-value.xlsx"),
                scenarios(new Object[][]{{1, "Y", "Booking Flow", "Create booking"}}),
                scenarioSheet("Booking Flow", new Object[][]{
                        {"Create Booking", "Y", "", "", "", "BRS", "Active testcase"},
                        {"", "", "openUrl", "", formula("\"Hello\"&\" World\""), "", "Use ordinary formula result"}
                }),
                objectRepository(new Object[][]{})
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ResolvedStepContext step = builder(excelReader)
                    .build()
                    .get(0)
                    .getTestcases()
                    .get(0)
                    .getSteps()
                    .get(0);

            Assert.assertEquals(step.getRawValue(), "Hello World");
            Assert.assertEquals(step.getResolvedValue(), "Hello World");
        }
    }

    private ExecutionPlanBuilder builder(ExcelReader excelReader) {
        ScenarioReader scenarioReader = new ScenarioReader(excelReader);
        StepReader stepReader = new StepReader(excelReader);
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        return new ExecutionPlanBuilder(scenarioReader, stepReader, dataReader, objectRepositoryReader);
    }
}
