package com.automation.tests;

import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.models.Scenario;
import com.automation.models.TestStep;
import com.automation.tests.support.ValidationWorkbookFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;

import static com.automation.tests.support.ValidationWorkbookFactory.objectRepository;
import static com.automation.tests.support.ValidationWorkbookFactory.sheet;

public class ObjectRepositoryValidationTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-11-object-validation");

    @Test
    public void missingObjectRepositorySheetShouldFailClearly() throws IOException {
        Path workbookPath = workbook("missing-object-repository.xlsx",
                sheet("LOGIN_DATA", new String[]{"NO", "USERNAME"}, new Object[][]{{1, "brs_admin"}}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                reader(excelReader).validateObjectRepository();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Required sheet not found: OBJECT_REPOSITORY.");
    }

    @Test
    public void missingXpathHeaderShouldFailClearly() throws IOException {
        Path workbookPath = workbook("missing-xpath-header.xlsx",
                sheet("OBJECT_REPOSITORY", new String[]{"Application", "Object"}, new Object[][]{{"BRS", "btnLogin"}}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                reader(excelReader).validateObjectRepository();
            }
        });

        Assert.assertEquals(exception.getMessage(), "Header not found: XPath in sheet OBJECT_REPOSITORY.");
    }

    @Test
    public void blankApplicationShouldFailClearly() throws IOException {
        assertRepositoryFails("blank-application.xlsx",
                new Object[][]{{"", "btnLogin", "//button[@id='login']", "Login"}},
                "Application is required in OBJECT_REPOSITORY row 2.");
    }

    @Test
    public void blankObjectShouldFailClearly() throws IOException {
        assertRepositoryFails("blank-object.xlsx",
                new Object[][]{{"BRS", "", "//button[@id='login']", "Login"}},
                "Object is required in OBJECT_REPOSITORY row 2.");
    }

    @Test
    public void blankXpathShouldFailClearly() throws IOException {
        assertRepositoryFails("blank-xpath.xlsx",
                new Object[][]{{"BRS", "btnLogin", "", "Login"}},
                "XPath is required in OBJECT_REPOSITORY row 2.");
    }

    @Test
    public void duplicateApplicationObjectShouldFailClearly() throws IOException {
        assertRepositoryFails("duplicate-object.xlsx",
                new Object[][]{
                        {"BRS", "btnLogin", "//button[@id='login']", "Login"},
                        {"brs", " BTNLOGIN ", "//button[@id='other']", "Duplicate"}
                },
                "Duplicate object found in OBJECT_REPOSITORY. Application = BRS, Object = btnLogin.");
    }

    @Test
    public void objectNotFoundShouldFailClearly() throws IOException {
        Path workbookPath = workbook("object-not-found.xlsx",
                objectRepository(new Object[][]{{"BRS", "btnLogin", "//button[@id='login']", "Login"}}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                reader(excelReader).getObject("BRS", "btnSubmitBooking");
            }
        });

        Assert.assertEquals(exception.getMessage(), "Object not found in OBJECT_REPOSITORY. Application = BRS, Object = btnSubmitBooking.");
    }

    @Test
    public void placeholderRequiresValueShouldFailClearly() throws IOException {
        Path workbookPath = workbook("placeholder-value.xlsx",
                objectRepository(new Object[][]{{"BRS", "btnRoomByName", "//button[contains(text(),'{ROOM_NAME}')]", "Room"}}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                reader(excelReader).resolveObject(step("btnRoomByName", ""), scenario());
            }
        });

        Assert.assertEquals(exception.getMessage(), "XPath placeholder {ROOM_NAME} requires a value for object btnRoomByName.");
    }

    @Test
    public void multiplePlaceholdersShouldFailClearly() throws IOException {
        Path workbookPath = workbook("multiple-placeholders.xlsx",
                objectRepository(new Object[][]{{"BRS", "btnRoomByName", "//button[@data-room='{ROOM_NAME}' and @data-date='{DATE}']", "Room"}}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                reader(excelReader).resolveObject(step("btnRoomByName", "Meeting Room A"), scenario());
            }
        });

        Assert.assertEquals(exception.getMessage(), "Multiple XPath placeholders are not supported in Phase 11 for object btnRoomByName.");
    }

    @Test
    public void blankPlaceholderShouldFailClearly() throws IOException {
        Path workbookPath = workbook("blank-placeholder.xlsx",
                objectRepository(new Object[][]{{"BRS", "btnRoomByName", "//button[contains(text(),'{}')]", "Room"}}));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                reader(excelReader).resolveObject(step("btnRoomByName", "Meeting Room A"), scenario());
            }
        });

        Assert.assertEquals(exception.getMessage(), "XPath placeholder cannot be blank for object btnRoomByName.");
    }

    private void assertRepositoryFails(String fileName, Object[][] rows, String expectedMessage) throws IOException {
        Path workbookPath = workbook(fileName, objectRepository(rows));

        IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class, () -> {
            try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
                reader(excelReader).validateObjectRepository();
            }
        });

        Assert.assertEquals(exception.getMessage(), expectedMessage);
    }

    private ObjectRepositoryReader reader(ExcelReader excelReader) {
        return new ObjectRepositoryReader(excelReader, new DataReader(excelReader));
    }

    private Scenario scenario() {
        return new Scenario("1", true, "Local Keyword Test", "Validation scenario", 2);
    }

    private TestStep step(String objectName, String value) {
        return new TestStep("1", "Validation scenario", "Local Keyword Test", "Create Booking",
                "click", objectName, value, "BRS", "Validation step", 5, 1);
    }

    private Path workbook(String fileName, ValidationWorkbookFactory.SheetData... sheets) throws IOException {
        return ValidationWorkbookFactory.createWorkbook(TEMP_DIR.resolve(fileName), sheets);
    }
}
