package com.automation.tests;

import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.ResolvedObject;
import com.automation.models.Scenario;
import com.automation.models.TestObject;
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

public class ObjectRepositoryReaderTest {

    private static final Path TEMPLATE_FILE = Path.of("src", "test", "resources", "testdata", "Template Testing.xlsx");
    private static final Path TEMP_DIR = Path.of("target", "object-repository-reader-test");

    @BeforeClass
    public void createTempDirectory() throws IOException {
        Files.createDirectories(TEMP_DIR);
    }

    @Test
    public void shouldReadAllObjectsFromTemplateWorkbook() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            List<TestObject> objects = objectRepositoryReader.getAllObjects();

            Assert.assertEquals(objects.size(), 13);
        }
    }

    @Test
    public void shouldResolveStaticXPath() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            TestObject testObject = objectRepositoryReader.getObject("BRS", "btnLogin");

            Assert.assertEquals(testObject.getXpath(), "//button[@id='login']");
        }
    }

    @Test
    public void sameObjectNameShouldResolveByApplication() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            TestObject brsLoginButton = objectRepositoryReader.getObject("BRS", "btnLogin");
            TestObject hrisLoginButton = objectRepositoryReader.getObject("HRIS", "btnLogin");
            TestObject brsUsernameField = objectRepositoryReader.getObject("BRS", "txtUsername");
            TestObject hrisUsernameField = objectRepositoryReader.getObject("HRIS", "txtUsername");

            Assert.assertEquals(brsLoginButton.getXpath(), "//button[@id='login']");
            Assert.assertEquals(hrisLoginButton.getXpath(), "//button[@id='login']");
            Assert.assertEquals(brsLoginButton.getApplication(), "BRS");
            Assert.assertEquals(hrisLoginButton.getApplication(), "HRIS");
            Assert.assertNotEquals(brsUsernameField.getXpath(), hrisUsernameField.getXpath());
        }
    }

    @Test
    public void lookupShouldIgnoreCaseAndTrimSpaces() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            TestObject testObject = objectRepositoryReader.getObject(" brs ", " BTNLOGIN ");

            Assert.assertEquals(testObject.getXpath(), "//button[@id='login']");
        }
    }

    @Test
    public void shouldResolveDynamicXPathUsingDataReferenceForScenarioOne() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            ResolvedObject resolvedObject = objectRepositoryReader.resolveObject(
                    step("1", "BRS", "btnRoomByName", "BOOKING_DATA.ROOM_NAME"),
                    scenario("1")
            );

            Assert.assertEquals(resolvedObject.getRawXpath(), "//button[contains(text(),'{ROOM_NAME}')]");
            Assert.assertEquals(resolvedObject.getResolvedValue(), "Meeting Room A");
            Assert.assertEquals(resolvedObject.getResolvedXpath(), "//button[contains(text(),'Meeting Room A')]");
        }
    }

    @Test
    public void shouldResolveDynamicXPathUsingDataReferenceForScenarioTwo() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            String resolvedXpath = objectRepositoryReader.resolveXPath(
                    step("2", "BRS", "btnRoomByName", "BOOKING_DATA.ROOM_NAME"),
                    scenario("2")
            );

            Assert.assertEquals(resolvedXpath, "//button[contains(text(),'Meeting Room B')]");
        }
    }

    @Test
    public void shouldResolveDynamicXPathUsingLiteralValue() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            ResolvedObject resolvedObject = objectRepositoryReader.resolveObject(
                    step("1", "BRS", "btnRoomByName", "Meeting Room A"),
                    scenario("1")
            );

            Assert.assertEquals(resolvedObject.getRawValue(), "Meeting Room A");
            Assert.assertEquals(resolvedObject.getResolvedValue(), "Meeting Room A");
            Assert.assertEquals(resolvedObject.getResolvedXpath(), "//button[contains(text(),'Meeting Room A')]");
        }
    }

    @Test
    public void xpathWithoutPlaceholderShouldRemainUnchanged() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            ResolvedObject resolvedObject = objectRepositoryReader.resolveObject(
                    step("1", "BRS", "btnLogin", ""),
                    scenario("1")
            );

            Assert.assertEquals(resolvedObject.getRawXpath(), "//button[@id='login']");
            Assert.assertEquals(resolvedObject.getResolvedXpath(), "//button[@id='login']");
        }
    }

    @Test
    public void blankObjectShouldReturnNullResolvedObject() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            ResolvedObject resolvedObject = objectRepositoryReader.resolveObject(
                    step("1", "BRS", "", "BOOKING_DATA.ROOM_NAME"),
                    scenario("1")
            );

            Assert.assertNull(resolvedObject);
            Assert.assertEquals(objectRepositoryReader.resolveXPath(step("1", "BRS", "", ""), scenario("1")), "");
        }
    }

    @Test
    public void objectNotFoundShouldThrowClearError() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            IllegalArgumentException exception = Assert.expectThrows(
                    IllegalArgumentException.class,
                    () -> objectRepositoryReader.getObject("BRS", "btnMissing")
            );

            Assert.assertEquals(exception.getMessage(), "Object not found in OBJECT_REPOSITORY. Application = BRS, Object = btnMissing.");
        }
    }

    @Test
    public void missingApplicationWhileResolvingShouldThrowClearError() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            IllegalArgumentException exception = Assert.expectThrows(
                    IllegalArgumentException.class,
                    () -> objectRepositoryReader.resolveObject(step("1", "", "btnLogin", ""), scenario("1"))
            );

            Assert.assertEquals(exception.getMessage(), "Application is required to resolve object 'btnLogin'.");
        }
    }

    @Test
    public void duplicateObjectShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "duplicate-object.xlsx",
                objectRepositorySheet(new Object[][]{
                        {"BRS", "btnLogin", "//button[@id='login']", "Login button"},
                        {"brs", " BTNLOGIN ", "//button[@id='duplicate']", "Duplicate login button"}
                })
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                objectRepositoryReader(excelReader).validateObjectRepository();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Duplicate object found in OBJECT_REPOSITORY: Application = brs, Object = BTNLOGIN.");
    }

    @Test
    public void missingObjectRepositorySheetShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-object-repository.xlsx",
                new SheetData("LOGIN_DATA", new String[]{"NO", "USERNAME"}, new Object[][]{{1, "brs_admin"}})
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                objectRepositoryReader(excelReader).validateObjectRepository();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Sheet not found: OBJECT_REPOSITORY");
    }

    @Test
    public void missingRequiredHeaderShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-xpath-header.xlsx",
                new SheetData("OBJECT_REPOSITORY", new String[]{"Application", "Object"}, new Object[][]{{"BRS", "btnLogin"}})
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                objectRepositoryReader(excelReader).validateObjectRepository();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Header not found: XPath in sheet OBJECT_REPOSITORY.");
    }

    @Test
    public void missingApplicationInObjectRowShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-application-row.xlsx",
                objectRepositorySheet(new Object[][]{{"", "btnLogin", "//button[@id='login']", "Login button"}})
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                objectRepositoryReader(excelReader).validateObjectRepository();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Application is required in OBJECT_REPOSITORY row 2.");
    }

    @Test
    public void missingObjectInObjectRowShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-object-row.xlsx",
                objectRepositorySheet(new Object[][]{{"BRS", "", "//button[@id='login']", "Login button"}})
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                objectRepositoryReader(excelReader).validateObjectRepository();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Object is required in OBJECT_REPOSITORY row 2.");
    }

    @Test
    public void missingXPathInObjectRowShouldThrowClearError() throws IOException {
        Path workbookPath = createWorkbook(
                "missing-xpath-row.xlsx",
                objectRepositorySheet(new Object[][]{{"BRS", "btnLogin", "", "Login button"}})
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                objectRepositoryReader(excelReader).validateObjectRepository();
            }
        });

        Assert.assertEquals(exception.getMessage(), "XPath is required in OBJECT_REPOSITORY row 2.");
    }

    @Test
    public void placeholderRequiresValueShouldThrowClearError() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            IllegalArgumentException exception = Assert.expectThrows(
                    IllegalArgumentException.class,
                    () -> objectRepositoryReader.resolveObject(step("1", "BRS", "btnRoomByName", ""), scenario("1"))
            );

            Assert.assertEquals(exception.getMessage(), "XPath placeholder {ROOM_NAME} requires a value for object btnRoomByName.");
        }
    }

    @Test
    public void multiplePlaceholdersShouldThrowUnsupportedError() throws IOException {
        Path workbookPath = createWorkbook(
                "multiple-placeholders.xlsx",
                objectRepositorySheet(new Object[][]{
                        {"BRS", "btnRoomByName", "//div[@data-room='{ROOM_NAME}' and @data-date='{BOOKING_DATE}']", "Dynamic room"}
                })
        );

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                objectRepositoryReader(excelReader).resolveObject(
                        step("1", "BRS", "btnRoomByName", "Meeting Room A"),
                        scenario("1")
                );
            }
        });

        Assert.assertEquals(exception.getMessage(), "Multiple XPath placeholders are not supported in Phase 6 for object btnRoomByName.");
    }

    @Test
    public void shouldResolveObjectFromParsedTestStepValues() {
        try (ExcelReader excelReader = new ExcelReader(TEMPLATE_FILE.toString())) {
            Scenario scenario = new ScenarioReader(excelReader).getActiveScenarios().get(0);
            TestStep roomSelectionStep = new StepReader(excelReader).getActiveSteps(scenario).stream()
                    .filter(step -> "btnRoomByName".equals(step.getObject()))
                    .findFirst()
                    .orElseThrow();
            ObjectRepositoryReader objectRepositoryReader = objectRepositoryReader(excelReader);

            ResolvedObject resolvedObject = objectRepositoryReader.resolveObject(roomSelectionStep, scenario);

            Assert.assertEquals(roomSelectionStep.getValue(), "BOOKING_DATA.ROOM_NAME");
            Assert.assertEquals(resolvedObject.getResolvedValue(), "Meeting Room A");
            Assert.assertEquals(resolvedObject.getResolvedXpath(), "//button[contains(text(),'Meeting Room A')]");
        }
    }

    private ObjectRepositoryReader objectRepositoryReader(ExcelReader excelReader) {
        return new ObjectRepositoryReader(excelReader, new DataReader(excelReader));
    }

    private Scenario scenario(String no) {
        return new Scenario(no, true, "Create New Booking", "Create booking room " + no, 2);
    }

    private TestStep step(String scenarioNo, String application, String objectName, String value) {
        return new TestStep(
                scenarioNo,
                "Create booking room " + scenarioNo,
                "Create New Booking",
                "Create Booking",
                "click",
                objectName,
                value,
                application,
                "Object repository test step",
                2,
                1
        );
    }

    private Path createWorkbook(String fileName, SheetData... sheets) throws IOException {
        Path workbookPath = TEMP_DIR.resolve(fileName);
        try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbookPath)) {
            for (SheetData sheetData : sheets) {
                Sheet sheet = workbook.createSheet(sheetData.name());
                writeRow(sheet.createRow(0), sheetData.headers());
                for (int rowIndex = 0; rowIndex < sheetData.rows().length; rowIndex++) {
                    writeRow(sheet.createRow(rowIndex + 1), sheetData.rows()[rowIndex]);
                }
            }
            workbook.write(outputStream);
        }
        return workbookPath;
    }

    private SheetData objectRepositorySheet(Object[][] rows) {
        return new SheetData(
                "OBJECT_REPOSITORY",
                new String[]{"Application", "Object", "XPath", "Description"},
                rows
        );
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

    private record SheetData(String name, String[] headers, Object[][] rows) {
    }
}
