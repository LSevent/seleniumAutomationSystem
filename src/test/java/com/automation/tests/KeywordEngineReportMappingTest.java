package com.automation.tests;

import com.automation.context.StepContextHolder;
import com.automation.engine.FunctionResolver;
import com.automation.engine.KeywordEngine;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.models.ExecutionResult;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.FunctionSourceType;
import com.automation.models.ResolvedStepContext;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;

public class KeywordEngineReportMappingTest {

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
    public void executionResultShouldPreserveResolvedStepReportFields() {
        ResolvedStepContext step = new ResolvedStepContext(
                "13",
                "Resolved Report Flow",
                "Resolved report mapping",
                "Resolved Report Flow",
                "Mapping Testcase",
                20,
                23,
                4,
                "mappingKeyword",
                "btnMapped",
                "BRS",
                "Preserve every resolved field",
                "DATA.RAW_VALUE",
                "resolved value",
                "//button[@data-state='{STATE}']",
                "//button[@data-state='ready']",
                ""
        );

        ExecutionResult result = engine().execute(step);

        Assert.assertEquals(result.getScenarioNo(), step.getScenarioNo());
        Assert.assertEquals(result.getScenarioName(), step.getScenarioName());
        Assert.assertEquals(result.getScenarioAction(), step.getScenarioAction());
        Assert.assertEquals(result.getTestcaseName(), step.getTestcaseName());
        Assert.assertEquals(result.getStepOrder(), step.getStepNumber());
        Assert.assertEquals(result.getExcelRowNumber(), step.getExcelRow());
        Assert.assertEquals(result.getDescription(), step.getDescription());
        Assert.assertEquals(result.getFunctionName(), step.getFunction());
        Assert.assertEquals(result.getObjectName(), step.getObjectName());
        Assert.assertEquals(result.getApplication(), step.getApplication());
        Assert.assertEquals(result.getRawValue(), step.getRawValue());
        Assert.assertEquals(result.getResolvedValue(), step.getResolvedValue());
        Assert.assertEquals(result.getRawXpath(), step.getRawXPath());
        Assert.assertEquals(result.getResolvedXpath(), step.getResolvedXPath());
        Assert.assertEquals(result.getExecutedByClass(), "mapping.Executor");
        Assert.assertEquals(result.getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.getEvidence(), "");
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    private KeywordEngine engine() {
        excelReader = new ExcelReader(TEMPLATE_FILE.toString());
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        return new KeywordEngine(
                dataReader,
                objectRepositoryReader,
                new MappingResolver(new FakeWebDriver())
        );
    }

    private static class MappingResolver extends FunctionResolver {

        private MappingResolver(FakeWebDriver driver) {
            super(driver.driver());
        }

        @Override
        public FunctionExecutionResult execute(
                String application,
                String functionName,
                String resolvedXpath,
                String resolvedValue
        ) {
            Assert.assertTrue(StepContextHolder.current().isPresent());
            return new FunctionExecutionResult(
                    application,
                    functionName,
                    "mapping.Executor",
                    FunctionSourceType.BASE,
                    true,
                    "Mapped resolved step."
            );
        }
    }
}
