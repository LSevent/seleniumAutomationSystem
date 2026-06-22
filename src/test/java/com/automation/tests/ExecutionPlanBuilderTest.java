package com.automation.tests;

import com.automation.engine.ExecutionPlanBuilder;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.exceptions.FrameworkException;
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
    public void mismatchedDynamicXpathPlaceholderShouldFailWithStepContext() throws IOException {
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

    private ExecutionPlanBuilder builder(ExcelReader excelReader) {
        ScenarioReader scenarioReader = new ScenarioReader(excelReader);
        StepReader stepReader = new StepReader(excelReader);
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        return new ExecutionPlanBuilder(scenarioReader, stepReader, dataReader, objectRepositoryReader);
    }
}
