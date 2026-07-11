package com.automation.tests;

import com.automation.engine.ExecutionPlanBuilder;
import com.automation.engine.KeywordResolver;
import com.automation.engine.KeywordEngine;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.ExecutionResult;
import com.automation.models.KeywordSourceType;
import com.automation.models.ResolvedStepContext;
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
    public void executeShouldRunResolvedStepFromPlanAndCallBaseFunction() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ResolvedStepContext step = resolvedStepByObject(excelReader, "txtUsername");
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.execute(step);

            Assert.assertTrue(result.isSuccess(), result.getMessage());
            Assert.assertEquals(result.getResolvedValue(), "brs_admin");
            Assert.assertEquals(result.getRawXPath(), "//input[@id='username']");
            Assert.assertEquals(result.getResolvedXPath(), "//input[@id='username']");
            Assert.assertEquals(result.getExecutionSource(), KeywordSourceType.BASE.name());
            Assert.assertEquals(fakeDriver.element("//input[@id='username']").getValue(), "brs_admin");
        }
    }

    @Test
    public void executeShouldUseResolvedDynamicXPathFromPlan() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ResolvedStepContext step = resolvedStepByObject(excelReader, "btnRoomByName");
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.execute(step);

            Assert.assertTrue(result.isSuccess(), result.getMessage());
            Assert.assertEquals(result.getResolvedValue(), "Meeting Room A");
            Assert.assertEquals(result.getResolvedXPath(), "//button[contains(text(),'Meeting Room A')]");
            Assert.assertEquals(result.getExecutionSource(), KeywordSourceType.BASE.name());
            Assert.assertTrue(fakeDriver.element("//button[contains(text(),'Meeting Room A')]").isClicked());
        }
    }

    @Test
    public void executeShouldSupportLiteralValuesFromResolvedStep() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ResolvedStepContext step = resolvedStep(
                    "Create Booking",
                    "input",
                    "txtBookingTitle",
                    "Manual Booking Title",
                    "Manual Booking Title",
                    "//input[@id='bookingTitle']",
                    "//input[@id='bookingTitle']",
                    15,
                    1
            );
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.execute(step);

            Assert.assertTrue(result.isSuccess(), result.getMessage());
            Assert.assertEquals(result.getResolvedValue(), "Manual Booking Title");
            Assert.assertEquals(fakeDriver.element("//input[@id='bookingTitle']").getValue(), "Manual Booking Title");
        }
    }

    @Test
    public void executeShouldSupportBlankObjectForValueOnlyKeyword() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ResolvedStepContext step = resolvedStepByKeyword(excelReader, "openUrl");
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.execute(step);

            Assert.assertTrue(result.isSuccess(), result.getMessage());
            Assert.assertEquals(result.getObjectName(), "");
            Assert.assertEquals(result.getResolvedValue(), baseUrl);
            Assert.assertEquals(result.getResolvedXPath(), "");
            Assert.assertEquals(fakeDriver.getCurrentUrl(), baseUrl);
        }
    }

    @Test
    public void executeShouldReturnClearFailureForBlankKeyword() {
        FakeWebDriver fakeDriver = localPageDriver();
        try (ExcelReader excelReader = new ExcelReader(workbookPath.toString())) {
            ResolvedStepContext step = resolvedStep(
                    "Login BRS",
                    " ",
                    "btnLogin",
                    "",
                    "",
                    "//button[@id='loginButton']",
                    "//button[@id='loginButton']",
                    3,
                    1
            );
            KeywordEngine keywordEngine = keywordEngine(excelReader, fakeDriver);

            ExecutionResult result = keywordEngine.execute(step);

            Assert.assertFalse(result.isSuccess());
            Assert.assertTrue(result.getMessage().contains("Keyword is required in sheet Local Keyword Test row 3."));
            Assert.assertTrue(result.getMessage().contains("Scenario NO: 1."));
            Assert.assertTrue(result.getMessage().contains("Scenario ACTION: Local Keyword Test."));
            Assert.assertTrue(result.getMessage().contains("Testcase: Login BRS."));
        }
    }

    private KeywordEngine keywordEngine(ExcelReader excelReader, FakeWebDriver fakeDriver) {
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        KeywordResolver keywordResolver = new KeywordResolver(fakeDriver.driver());
        return new KeywordEngine(dataReader, objectRepositoryReader, keywordResolver);
    }

    private ResolvedStepContext resolvedStepByKeyword(ExcelReader excelReader, String keyword) {
        return resolvedSteps(excelReader).stream()
                .filter(step -> keyword.equals(step.getKeyword()))
                .findFirst()
                .orElseThrow();
    }

    private ResolvedStepContext resolvedStepByObject(ExcelReader excelReader, String object) {
        return resolvedSteps(excelReader).stream()
                .filter(step -> object.equals(step.getObjectName()))
                .findFirst()
                .orElseThrow();
    }

    private List<ResolvedStepContext> resolvedSteps(ExcelReader excelReader) {
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        return new ExecutionPlanBuilder(
                new ScenarioReader(excelReader),
                new StepReader(excelReader),
                dataReader,
                objectRepositoryReader
        ).build()
                .stream()
                .flatMap(scenario -> scenario.getTestcases().stream())
                .flatMap(testcase -> testcase.getSteps().stream())
                .toList();
    }

    private ResolvedStepContext resolvedStep(
            String testcaseName,
            String keyword,
            String object,
            String rawValue,
            String resolvedValue,
            String rawXPath,
            String resolvedXPath,
            int excelRowNumber,
            int stepOrder
    ) {
        return ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioAction("Local Keyword Test")
                .scenarioName("Local keyword execution test")
                .sheetName("Local Keyword Test")
                .testcaseName(testcaseName)
                .testcaseParentRow(2)
                .excelRow(excelRowNumber)
                .stepNumber(stepOrder)
                .keyword(keyword)
                .objectName(object)
                .application("BRS")
                .description("KeywordEngine test step")
                .rawValue(rawValue)
                .resolvedValue(resolvedValue)
                .rawXPath(rawXPath)
                .resolvedXPath(resolvedXPath)
                .executedBy("")
                .build();
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
