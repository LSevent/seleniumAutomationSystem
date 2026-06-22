package com.automation.tests;

import com.automation.context.StepContextHolder;
import com.automation.engine.FunctionResolver;
import com.automation.engine.KeywordEngine;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ExecutionResult;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.FunctionSourceType;
import com.automation.models.ResolvedStepContext;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;

@Test(singleThreaded = true)
public class StepContextLeakRegressionTest {

    private static final Path TEMPLATE_FILE = Path.of(
            "src", "test", "resources", "testdata", "Template Testing.xlsx"
    );

    private ExcelReader excelReader;

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        StepContextHolder.clear();
        if (excelReader != null) {
            excelReader.close();
            excelReader = null;
        }
    }

    @Test
    public void contextShouldBeClearedAfterEverySuccessfulStep() {
        ObservingResolver resolver = new ObservingResolver(new FakeWebDriver(), false);
        KeywordEngine engine = engine(resolver);
        ResolvedStepContext first = step(7, "firstKeyword", "btnFirst");
        ResolvedStepContext second = step(8, "secondKeyword", "btnSecond");

        Assert.assertTrue(engine.execute(first).isSuccess());
        Assert.assertSame(resolver.observedContext, first);
        Assert.assertTrue(StepContextHolder.current().isEmpty());

        Assert.assertTrue(engine.execute(second).isSuccess());
        Assert.assertSame(resolver.observedContext, second);
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    @Test
    public void contextShouldBeClearedAfterFailedStep() {
        ObservingResolver resolver = new ObservingResolver(new FakeWebDriver(), true);
        ResolvedStepContext step = step(9, "failingKeyword", "btnFailure");

        ExecutionResult result = engine(resolver).execute(step);

        Assert.assertFalse(result.isSuccess());
        Assert.assertSame(resolver.observedContext, step);
        Assert.assertTrue(result.getMessage().contains("Scenario NO: 1."));
        Assert.assertTrue(result.getMessage().contains("Row: 9."));
        Assert.assertTrue(result.getMessage().contains("Function: failingKeyword."));
        Assert.assertTrue(result.getMessage().contains("Object: btnFailure."));
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    private KeywordEngine engine(FunctionResolver resolver) {
        excelReader = new ExcelReader(TEMPLATE_FILE.toString());
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        return new KeywordEngine(dataReader, objectRepositoryReader, resolver);
    }

    private ResolvedStepContext step(int row, String function, String objectName) {
        return ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioAction("Local Keyword Test")
                .scenarioName("Context leak regression")
                .sheetName("Local Keyword Test")
                .testcaseName("Leak Testcase")
                .testcaseParentRow(4)
                .excelRow(row)
                .stepNumber(row - 6)
                .function(function)
                .objectName(objectName)
                .application("BRS")
                .description("Context cleanup step")
                .rawValue("raw")
                .resolvedValue("resolved")
                .rawXPath("//button[@id='raw']")
                .resolvedXPath("//button[@id='resolved']")
                .executedBy("")
                .build();
    }

    private static class ObservingResolver extends FunctionResolver {

        private final boolean fail;
        private ResolvedStepContext observedContext;

        private ObservingResolver(FakeWebDriver driver, boolean fail) {
            super(driver.driver());
            this.fail = fail;
        }

        @Override
        public FunctionExecutionResult execute(String application, String functionName) {
            observedContext = StepContextHolder.get();
            if (fail) {
                throw new FrameworkException("Synthetic resolved-plan failure.");
            }
            return new FunctionExecutionResult(
                    application,
                    functionName,
                    "leak-test.Executor",
                    FunctionSourceType.BASE,
                    true,
                    "Synthetic success."
            );
        }
    }
}
