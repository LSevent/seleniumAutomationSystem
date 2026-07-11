package com.automation.tests;

import com.automation.excel.ExcelReader;
import com.automation.excel.StepReader;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.models.TestStep;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class StepReaderTest {

    private static final Path TEMP_DIR = Path.of("target", "step-reader-test");
    private static final Path TEMPLATE_FILE = Path.of("src", "test", "resources", "testdata", "Template Testing.xlsx");
    private static final String CREATE_BOOKING_SHEET = "Create New Booking";
    private static final String[] REQUIRED_HEADERS = {
            "Testcase", "Run", "Keyword", "Object", "Value", "Application", "Description"
    };

    @BeforeClass
    public void createTempDirectory() throws IOException {
        Files.createDirectories(TEMP_DIR);
    }

    @Test
    public void shouldParseTestcaseBlocksFromScenarioSheet() throws IOException {
        Path workbookPath = createValidWorkbook("parse-testcases.xlsx");

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            List<TestCaseBlock> testCases = new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET));

            Assert.assertEquals(testCases.size(), 2);
            Assert.assertEquals(testCases.get(0).getTestcaseName(), "Login BRS");
            Assert.assertEquals(testCases.get(1).getTestcaseName(), "Create Booking");
        }
    }

    @Test
    public void legacyFunctionHeaderShouldStillBeSupported() throws IOException {
        Path workbookPath = createWorkbook(
                "legacy-function-header.xlsx",
                CREATE_BOOKING_SHEET,
                new String[]{"Testcase", "Run", "Function", "Object", "Value", "Application", "Description"},
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "", "BRS", "Login"},
                        {"", "", "click", "btnLogin", "", "", "Click login"}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            TestStep step = new StepReader(excelReader).getActiveSteps(scenario(CREATE_BOOKING_SHEET)).get(0);

            Assert.assertEquals(step.getKeyword(), "click");
        }
    }

    @Test
    public void keywordHeaderShouldWinWhenLegacyFunctionHeaderAlsoExists() throws IOException {
        Path workbookPath = createWorkbook(
                "keyword-preferred-over-function.xlsx",
                CREATE_BOOKING_SHEET,
                new String[]{"Testcase", "Run", "Keyword", "Function", "Object", "Value", "Application", "Description"},
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "", "", "BRS", "Login"},
                        {"", "", "click", "input", "btnLogin", "", "", "Click login"}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            TestStep step = new StepReader(excelReader).getActiveSteps(scenario(CREATE_BOOKING_SHEET)).get(0);

            Assert.assertEquals(step.getKeyword(), "click");
        }
    }

    @Test
    public void shouldParseCreateNewBookingFromTemplateWorkbook() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            List<TestCaseBlock> testCases = new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET));

            Assert.assertEquals(testCases.size(), 2);
            Assert.assertEquals(testCases.get(0).getTestcaseName(), "Login BRS");
            Assert.assertEquals(testCases.get(0).getSteps().size(), 4);
            Assert.assertEquals(testCases.get(1).getTestcaseName(), "Create Booking");
            Assert.assertEquals(testCases.get(1).getSteps().size(), 5);
        }
    }

    @Test
    public void shouldParseCancelBookingFromTemplateWorkbook() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            List<TestCaseBlock> testCases = new StepReader(excelReader).getTestCases(scenario("Cancel Booking"));

            Assert.assertEquals(testCases.size(), 2);
            Assert.assertEquals(testCases.get(0).getTestcaseName(), "Login BRS");
            Assert.assertEquals(testCases.get(0).getSteps().size(), 3);
            Assert.assertEquals(testCases.get(1).getTestcaseName(), "Cancel Booking");
            Assert.assertEquals(testCases.get(1).getSteps().size(), 3);
        }
    }

    @Test
    public void shouldReturnActiveTestcaseBlocksOnly() throws IOException {
        Path workbookPath = createWorkbook(
                "active-testcases.xlsx",
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "", "BRS", "Login to BRS"},
                        {"", "", "input", "txtUsername", "LOGIN_DATA.USERNAME", "", "Input username"},
                        {"Optional Setup", "No", "", "", "", "", "Inactive setup"},
                        {"", "", "click", "btnOptional", "", "", "Optional click"},
                        {"Create Booking", "True", "", "", "", "BRS", "Create booking"},
                        {"", "", "click", "btnSubmitBooking", "", "", "Submit"}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            List<TestCaseBlock> activeTestCases = new StepReader(excelReader).getActiveTestCases(scenario(CREATE_BOOKING_SHEET));

            Assert.assertEquals(activeTestCases.size(), 2);
            Assert.assertEquals(activeTestCases.get(0).getTestcaseName(), "Login BRS");
            Assert.assertEquals(activeTestCases.get(1).getTestcaseName(), "Create Booking");
        }
    }

    @Test
    public void shouldParseStepsInExcelRowOrder() throws IOException {
        Path workbookPath = createValidWorkbook("step-order.xlsx");

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            TestCaseBlock loginTestCase = new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET)).get(0);

            Assert.assertEquals(loginTestCase.getSteps().get(0).getKeyword(), "input");
            Assert.assertEquals(loginTestCase.getSteps().get(1).getKeyword(), "input");
            Assert.assertEquals(loginTestCase.getSteps().get(2).getKeyword(), "click");
            Assert.assertEquals(loginTestCase.getSteps().get(0).getStepOrder(), 1);
            Assert.assertEquals(loginTestCase.getSteps().get(1).getStepOrder(), 2);
            Assert.assertEquals(loginTestCase.getSteps().get(2).getStepOrder(), 3);
        }
    }

    @Test
    public void stepRowsShouldInheritApplicationFromParent() throws IOException {
        Path workbookPath = createValidWorkbook("inherit-application.xlsx");

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            TestCaseBlock loginTestCase = new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET)).get(0);

            Assert.assertTrue(loginTestCase.getSteps().stream().allMatch(step -> "BRS".equals(step.getApplication())));
        }
    }

    @Test
    public void shouldParseScenarioSheetByHeaderNameWhenColumnsAreReordered() throws IOException {
        Path workbookPath = createWorkbook(
                "reordered-step-columns.xlsx",
                CREATE_BOOKING_SHEET,
                new String[]{" keyword ", " object ", " value ", " testcase ", " run ", " application ", " description "},
                new Object[][]{
                        {"", "", "", "Login BRS", "Yes", "BRS", "Login to BRS"},
                        {"input", "txtUsername", "LOGIN_DATA.USERNAME", "", "", "", "Input username"},
                        {"click", "btnLogin", "", "", "", "", "Click login"}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            TestCaseBlock loginTestCase = new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET)).get(0);
            List<TestStep> steps = loginTestCase.getSteps();

            Assert.assertEquals(loginTestCase.getTestcaseName(), "Login BRS");
            Assert.assertEquals(loginTestCase.getApplication(), "BRS");
            Assert.assertEquals(steps.size(), 2);
            Assert.assertEquals(steps.get(0).getKeyword(), "input");
            Assert.assertEquals(steps.get(0).getObject(), "txtUsername");
            Assert.assertEquals(steps.get(0).getValue(), "LOGIN_DATA.USERNAME");
            Assert.assertEquals(steps.get(0).getApplication(), "BRS");
            Assert.assertEquals(steps.get(0).getDescription(), "Input username");
            Assert.assertEquals(steps.get(1).getKeyword(), "click");
            Assert.assertEquals(steps.get(1).getStepOrder(), 2);
        }
    }

    @Test
    public void stepRowApplicationShouldOverrideParentApplication() throws IOException {
        Path workbookPath = createWorkbook(
                "override-application.xlsx",
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "", "BRS", "Login to BRS"},
                        {"", "", "input", "txtUsername", "LOGIN_DATA.USERNAME", "HRIS", "Input username"}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            TestStep step = new StepReader(excelReader).getActiveSteps(scenario(CREATE_BOOKING_SHEET)).get(0);

            Assert.assertEquals(step.getApplication(), "HRIS");
        }
    }

    @Test
    public void descriptionHeaderShouldBeOptional() throws IOException {
        Path workbookPath = createWorkbook(
                "description-optional.xlsx",
                CREATE_BOOKING_SHEET,
                new String[]{"Testcase", "Run", "Keyword", "Object", "Value", "Application"},
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "", "BRS"},
                        {"", "", "click", "btnLogin", "", ""}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            TestStep step = new StepReader(excelReader).getActiveSteps(scenario(CREATE_BOOKING_SHEET)).get(0);

            Assert.assertEquals(step.getDescription(), "");
        }
    }

    @Test
    public void stepRowBeforeTestcaseShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "step-before-testcase.xlsx",
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"", "", "input", "txtUsername", "LOGIN_DATA.USERNAME", "", "Input username"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET));
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Step row found before any testcase parent row. Sheet: Create New Booking. Row: 2."));
    }

    @Test
    public void missingRequiredHeaderShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-keyword-header.xlsx",
                CREATE_BOOKING_SHEET,
                new String[]{"Testcase", "Run", "Object", "Value", "Application", "Description"},
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "BRS", "Login to BRS"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET));
            }
        });

        Assert.assertEquals(
                exception.getMessage(),
                "Missing required column 'Keyword' in sheet 'Create New Booking'. "
                        + "Legacy column 'Function' is also supported, but neither was found."
        );
    }

    @Test
    public void activeTestcaseWithoutApplicationShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-application.xlsx",
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "", "", "Login to BRS"},
                        {"", "", "click", "btnLogin", "", "", "Click login"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET));
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Application is required for active testcase 'Login BRS'. Sheet: Create New Booking. Row: 2."));
    }

    @Test
    public void invalidRunValueShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "invalid-run.xlsx",
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"Login BRS", "MAYBE", "", "", "", "BRS", "Login to BRS"},
                        {"", "", "click", "btnLogin", "", "", "Click login"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET));
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Invalid Run value 'MAYBE' in sheet Create New Booking row 2."));
    }

    @Test
    public void stepRunShouldDefaultToYesAndSupportExplicitYesOrNo() throws IOException {
        Path workbookPath = createWorkbook(
                "step-run-values.xlsx",
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "", "BRS", "Login to BRS"},
                        {"", "", "click", "btnLogin", "", "", "Blank inherits active testcase"},
                        {"", "No", "input", "txtOptional", "optional", "", "Explicitly skipped"},
                        {"", "TRUE", "clear", "txtUsername", "", "", "Explicitly active"}
                }
        );

        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            List<TestStep> steps = new StepReader(excelReader)
                    .getActiveSteps(scenario(CREATE_BOOKING_SHEET));

            Assert.assertTrue(steps.get(0).isRun());
            Assert.assertFalse(steps.get(1).isRun());
            Assert.assertTrue(steps.get(2).isRun());
        }
    }

    @Test
    public void invalidStepRunValueShouldFailClearly() throws IOException {
        Path workbookPath = createWorkbook(
                "invalid-step-run.xlsx",
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "", "BRS", "Login to BRS"},
                        {"", "MAYBE", "click", "btnLogin", "", "", "Invalid step Run"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET));
            }
        });

        Assert.assertTrue(exception.getMessage().contains(
                "Invalid Run value 'MAYBE' for step in sheet Create New Booking row 3."
        ));
    }

    @Test
    public void flowDirectiveCannotBeDisabledWithStepRun() throws IOException {
        Path workbookPath = createWorkbook(
                "disabled-flow-directive.xlsx",
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"Booking", "Yes", "", "", "", "BRS", "Booking flow"},
                        {"", "No", "ifEquals", "", "CONFIG.RUN = Yes", "", "Invalid disabled directive"},
                        {"", "", "endIf", "", "", "", "End condition"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET));
            }
        });

        Assert.assertTrue(exception.getMessage().contains(
                "Run=No is not supported for flow directive 'ifEquals' in sheet Create New Booking row 3."
        ));
    }

    @Test
    public void activeTestcaseWithNoStepsShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "no-steps.xlsx",
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "", "BRS", "Login to BRS"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET));
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Active testcase 'Login BRS' has no steps. Sheet: Create New Booking. Row: 2."));
    }

    @Test
    public void testcaseParentRowWithStepFieldsShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "parent-with-step-fields.xlsx",
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"Login BRS", "Yes", "click", "", "", "BRS", "Login to BRS"},
                        {"", "", "click", "btnLogin", "", "", "Click login"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET));
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Testcase parent row should not contain Keyword, Object, or Value. Sheet: Create New Booking. Row: 2."));
    }

    @Test
    public void duplicateTestcaseNameShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "duplicate-testcase.xlsx",
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "", "BRS", "Login to BRS"},
                        {"", "", "click", "btnLogin", "", "", "Click login"},
                        {"Login BRS", "No", "", "", "", "", "Duplicate login"}
                }
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                new StepReader(excelReader).getTestCases(scenario(CREATE_BOOKING_SHEET));
            }
        });

        Assert.assertTrue(exception.getMessage().contains("Duplicate testcase name 'Login BRS' found in sheet Create New Booking."));
    }

    private Path createValidWorkbook(String fileName) throws IOException {
        return createWorkbook(
                fileName,
                CREATE_BOOKING_SHEET,
                REQUIRED_HEADERS,
                new Object[][]{
                        {"Login BRS", "Yes", "", "", "", "BRS", "Login to BRS"},
                        {"", "", "input", "txtUsername", "LOGIN_DATA.USERNAME", "", "Input username"},
                        {"", "", "input", "txtPassword", "LOGIN_DATA.PASSWORD", "", "Input password"},
                        {"", "", "click", "btnLogin", "", "", "Click login"},
                        {"", "", "verifyDisplayed", "lblDashboard", "", "", "Verify dashboard"},
                        {"Create Booking", "Yes", "", "", "", "BRS", "Create booking"},
                        {"", "", "click", "menuBooking", "", "", "Open booking menu"},
                        {"", "", "input", "txtBookingTitle", "BOOKING_DATA.BOOKING_TITLE", "", "Input title"},
                        {"", "", "click", "btnRoomByName", "BOOKING_DATA.ROOM_NAME", "", "Select room"},
                        {"", "", "click", "btnSubmitBooking", "", "", "Submit"},
                        {"", "", "verifyText", "lblSuccessMessage", "BOOKING_DATA.EXPECTED_MESSAGE", "", "Verify success"}
                }
        );
    }

    private Path createWorkbook(String fileName, String sheetName, String[] headers, Object[][] rows) throws IOException {
        Path workbookPath = TEMP_DIR.resolve(fileName);
        try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbookPath)) {
            Sheet sheet = workbook.createSheet(sheetName);
            writeRow(sheet.createRow(0), headers);
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                writeRow(sheet.createRow(rowIndex + 1), rows[rowIndex]);
            }
            workbook.write(outputStream);
        }
        return workbookPath;
    }

    private Scenario scenario(String action) {
        return new Scenario("1", true, action, "Create booking room A", 2);
    }

    private void writeRow(Row row, Object[] values) {
        for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
            Object value = values[columnIndex];
            if (value instanceof Number number) {
                row.createCell(columnIndex).setCellValue(number.doubleValue());
            } else if (value instanceof Boolean bool) {
                row.createCell(columnIndex).setCellValue(bool);
            } else if (value != null) {
                row.createCell(columnIndex).setCellValue(value.toString());
            }
        }
    }
}
