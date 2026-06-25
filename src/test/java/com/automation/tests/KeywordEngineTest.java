package com.automation.tests;

import com.automation.engine.FunctionResolver;
import com.automation.engine.KeywordEngine;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.ExecutionResult;
import com.automation.models.KeywordSourceType;
import com.automation.models.Scenario;
import com.automation.models.TestStep;
import com.automation.tests.support.ExcelKeywordTestWorkbookFactory;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class KeywordEngineTest {

    private static final Path TEMP_DIR = Path.of("target", "keyword-engine-test");
    private static final Path LOCAL_HTML = Path.of("src", "test", "resources", "test-pages", "excel-keyword-test.html");
    private Path workbookPath;
    private String baseUrl;

    @BeforeClass
    public void createWorkbook() throws IOException {
        baseUrl = LOCAL_HTML.toAbsolutePath().toUri().toString();
        workbookPath = ExcelKeywordTestWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("excel-keyword-engine-test.xlsx"),
                baseUrl
        );
    }

    @Test
    public void executeStepShouldResolveValueObjectAndCallBaseFunction() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            Scenario scenario = activeScenario(excelReader);
            TestStep step = stepByObject(excelReader, scenario, "txtUsername");
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.executeStep(scenario, step);

            Assert.assertTrue(result.isSuccess(), result.getMessage());
            Assert.assertEquals(result.getResolvedValue(), "brs_admin");
            Assert.assertEquals(result.getRawXPath(), "//input[@id='username']");
            Assert.assertEquals(result.getResolvedXPath(), "//input[@id='username']");
            Assert.assertEquals(result.getExecutionSource(), KeywordSourceType.BASE.name());
            Assert.assertEquals(fakeDriver.element("//input[@id='username']").getValue(), "brs_admin");
        }
    }

    @Test
    public void executeStepShouldResolveDynamicXPathPlaceholder() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            Scenario scenario = activeScenario(excelReader);
            TestStep step = stepByObject(excelReader, scenario, "btnRoomByName");
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.executeStep(scenario, step);

            Assert.assertTrue(result.isSuccess(), result.getMessage());
            Assert.assertEquals(result.getResolvedValue(), "Meeting Room A");
            Assert.assertEquals(result.getResolvedXPath(), "//button[contains(text(),'Meeting Room A')]");
            Assert.assertEquals(result.getExecutionSource(), KeywordSourceType.SPECIFIC.name());
            Assert.assertTrue(fakeDriver.element("//button[contains(text(),'Meeting Room A')]").isClicked());
        }
    }

    @Test
    public void executeStepShouldSupportLiteralValues() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            Scenario scenario = activeScenario(excelReader);
            TestStep step = step("Create Booking", "input", "txtBookingTitle", "Manual Booking Title", 15, 1);
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.executeStep(scenario, step);

            Assert.assertTrue(result.isSuccess(), result.getMessage());
            Assert.assertEquals(result.getResolvedValue(), "Manual Booking Title");
            Assert.assertEquals(fakeDriver.element("//input[@id='bookingTitle']").getValue(), "Manual Booking Title");
        }
    }

    @Test
    public void executeStepShouldSupportBlankObjectForValueOnlyKeyword() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            Scenario scenario = activeScenario(excelReader);
            TestStep step = stepByKeyword(excelReader, scenario, "openUrl");
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.executeStep(scenario, step);

            Assert.assertTrue(result.isSuccess(), result.getMessage());
            Assert.assertEquals(result.getObjectName(), "");
            Assert.assertEquals(result.getResolvedValue(), baseUrl);
            Assert.assertEquals(result.getResolvedXPath(), "");
            Assert.assertEquals(fakeDriver.getCurrentUrl(), baseUrl);
        }
    }

    @Test
    public void executeStepShouldReturnClearFailureForBlankKeyword() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            Scenario scenario = activeScenario(excelReader);
            TestStep step = step("Login BRS", " ", "btnLogin", "", 3, 1);
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.executeStep(scenario, step);

            Assert.assertFalse(result.isSuccess());
            Assert.assertEquals(result.getMessage(), "Keyword is required in sheet Local Keyword Test row 3.");
        }
    }

    @Test
    public void executeStepShouldReturnClearFailureForDataReferenceError() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            Scenario scenario = activeScenario(excelReader);
            TestStep step = step("Login BRS", "input", "txtUsername", "MISSING_DATA.USERNAME", 4, 1);
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.executeStep(scenario, step);

            Assert.assertFalse(result.isSuccess());
            Assert.assertTrue(result.getMessage().contains("Failed to resolve value for step row 4."));
            Assert.assertTrue(result.getMessage().contains("Raw value: MISSING_DATA.USERNAME."));
            Assert.assertTrue(result.getMessage().contains("Scenario NO: 1."));
            Assert.assertTrue(result.getMessage().contains("Scenario ACTION: Local Keyword Test."));
            Assert.assertTrue(result.getMessage().contains("Testcase: Login BRS."));
            Assert.assertTrue(result.getMessage().contains("Data sheet not found: MISSING_DATA."));
        }
    }

    @Test
    public void executeStepShouldReturnClearFailureForObjectResolutionError() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            Scenario scenario = activeScenario(excelReader);
            TestStep step = step("Login BRS", "click", "btnMissing", "", 5, 1);
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.executeStep(scenario, step);

            Assert.assertFalse(result.isSuccess());
            Assert.assertTrue(result.getMessage().contains("Failed to resolve object for step row 5."));
            Assert.assertTrue(result.getMessage().contains("Application: BRS."));
            Assert.assertTrue(result.getMessage().contains("Object: btnMissing."));
            Assert.assertTrue(result.getMessage().contains("Scenario NO: 1."));
            Assert.assertTrue(result.getMessage().contains("Testcase: Login BRS."));
            Assert.assertTrue(result.getMessage().contains("Object not found in OBJECT_REPOSITORY. Application = BRS, Object = btnMissing."));
        }
    }

    private KeywordEngine keywordEngine(ExcelReader excelReader, FakeWebDriver fakeDriver) {
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        FunctionResolver functionResolver = new FunctionResolver(fakeDriver.driver());
        return new KeywordEngine(dataReader, objectRepositoryReader, functionResolver);
    }

    private Scenario activeScenario(ExcelReader excelReader) {
        return new ScenarioReader(excelReader).getActiveScenarios().get(0);
    }

    private TestStep stepByKeyword(ExcelReader excelReader, Scenario scenario, String keyword) {
        return activeSteps(excelReader, scenario).stream()
                .filter(step -> keyword.equals(step.getKeyword()))
                .findFirst()
                .orElseThrow();
    }

    private TestStep stepByObject(ExcelReader excelReader, Scenario scenario, String object) {
        return activeSteps(excelReader, scenario).stream()
                .filter(step -> object.equals(step.getObject()))
                .findFirst()
                .orElseThrow();
    }

    private List<TestStep> activeSteps(ExcelReader excelReader, Scenario scenario) {
        return new StepReader(excelReader).getActiveSteps(scenario);
    }

    private TestStep step(String testcaseName, String keyword, String object, String value, int excelRowNumber, int stepOrder) {
        return new TestStep(
                "1",
                "Local keyword execution test",
                "Local Keyword Test",
                testcaseName,
                keyword,
                object,
                value,
                "BRS",
                "KeywordEngine test step",
                excelRowNumber,
                stepOrder
        );
    }

    private FakeWebDriver localPageDriver() {
        FakeWebDriver fakeDriver = new FakeWebDriver();
        fakeDriver.setTitle("Excel Keyword Test");
        fakeDriver.addElement("//input[@id='username']", "");
        fakeDriver.addElement("//input[@id='password']", "");
        fakeDriver.addElement("//button[@id='loginButton']", "Login");
        fakeDriver.addElement("//h1[@id='dashboard']", "Dashboard");
        fakeDriver.addElement("//input[@id='bookingTitle']", "");
        fakeDriver.addElement("//button[contains(text(),'Meeting Room A')]", "Meeting Room A");
        fakeDriver.addElement("//div[@id='message']", "Booking created successfully");
        return fakeDriver;
    }
}
