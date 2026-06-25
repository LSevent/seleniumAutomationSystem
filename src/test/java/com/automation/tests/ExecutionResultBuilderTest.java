package com.automation.tests;

import com.automation.models.ExecutionResult;
import com.automation.models.ResolvedStepContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ExecutionResultBuilderTest {

    private static final String RAW_SECRET = "superSecretPassword123";
    private static final String RESOLVED_SECRET = "sensitiveResolvedValue";

    @Test
    public void builderShouldCreateExecutionResultWithAllFields() {
        ExecutionResult result = ExecutionResult.builder()
                .scenarioNo("1")
                .scenarioName("Local keyword execution test")
                .scenarioAction("Local Keyword Test")
                .testcaseName("Login BRS")
                .description("Enter password")
                .keywordName("input")
                .objectName("txtPassword")
                .application("BRS")
                .rawValue(RAW_SECRET)
                .resolvedValue(RESOLVED_SECRET)
                .rawXpath("//input[@name='password']")
                .resolvedXpath("//input[@id='password']")
                .executedByClass("com.automation.base.BaseFunction")
                .executionSource("BASE")
                .success(true)
                .status(ExecutionResult.STATUS_PASS)
                .evidence("screenshots/login.png")
                .message("Step passed")
                .excelRowNumber(7)
                .stepOrder(2)
                .build();

        Assert.assertEquals(result.getScenarioNo(), "1");
        Assert.assertEquals(result.getScenarioName(), "Local keyword execution test");
        Assert.assertEquals(result.getScenarioAction(), "Local Keyword Test");
        Assert.assertEquals(result.getTestcaseName(), "Login BRS");
        Assert.assertEquals(result.getDescription(), "Enter password");
        Assert.assertEquals(result.getKeywordName(), "input");
        Assert.assertEquals(result.getObjectName(), "txtPassword");
        Assert.assertEquals(result.getApplication(), "BRS");
        Assert.assertEquals(result.getRawValue(), RAW_SECRET);
        Assert.assertEquals(result.getResolvedValue(), RESOLVED_SECRET);
        Assert.assertEquals(result.getRawXpath(), "//input[@name='password']");
        Assert.assertEquals(result.getResolvedXpath(), "//input[@id='password']");
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.base.BaseFunction");
        Assert.assertEquals(result.getExecutionSource(), "BASE");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.getEvidence(), "screenshots/login.png");
        Assert.assertEquals(result.getMessage(), "Step passed");
        Assert.assertEquals(result.getExcelRowNumber(), 7);
        Assert.assertEquals(result.getStepOrder(), 2);
    }

    @Test
    public void builderAliasesShouldMapToExistingFields() {
        ExecutionResult result = ExecutionResult.builder()
                .keyword("click")
                .executedBy("com.automation.functions.BRS.SpecificFunction")
                .excelRow(8)
                .stepNumber(3)
                .build();

        Assert.assertEquals(result.getKeywordName(), "click");
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.functions.BRS.SpecificFunction");
        Assert.assertEquals(result.getExcelRowNumber(), 8);
        Assert.assertEquals(result.getStepOrder(), 3);
    }

    @Test
    public void fromStepShouldMapResolvedStepContextFields() {
        ExecutionResult result = ExecutionResult.fromStep(resolvedStep())
                .success(false)
                .status(ExecutionResult.STATUS_FAIL)
                .executionSource("KEYWORD")
                .evidence("failure.png")
                .message("Step failed")
                .build();

        Assert.assertEquals(result.getScenarioNo(), "1");
        Assert.assertEquals(result.getScenarioName(), "Local keyword execution test");
        Assert.assertEquals(result.getScenarioAction(), "Local Keyword Test");
        Assert.assertEquals(result.getTestcaseName(), "Login BRS");
        Assert.assertEquals(result.getDescription(), "Enter password");
        Assert.assertEquals(result.getKeywordName(), "input");
        Assert.assertEquals(result.getObjectName(), "txtPassword");
        Assert.assertEquals(result.getApplication(), "BRS");
        Assert.assertEquals(result.getRawValue(), RAW_SECRET);
        Assert.assertEquals(result.getResolvedValue(), RESOLVED_SECRET);
        Assert.assertEquals(result.getRawXpath(), "//input[@name='password']");
        Assert.assertEquals(result.getResolvedXpath(), "//input[@id='password']");
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.base.BaseFunction");
        Assert.assertEquals(result.getExecutionSource(), "KEYWORD");
        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(result.getStatus(), ExecutionResult.STATUS_FAIL);
        Assert.assertEquals(result.getEvidence(), "failure.png");
        Assert.assertEquals(result.getMessage(), "Step failed");
        Assert.assertEquals(result.getExcelRowNumber(), 7);
        Assert.assertEquals(result.getStepOrder(), 2);
    }

    @Test
    public void toStringShouldRemainSensitiveDataSafe() {
        String text = ExecutionResult.builder()
                .keyword("input")
                .objectName("txtPassword")
                .application("BRS")
                .rawValue(RAW_SECRET)
                .resolvedValue(RESOLVED_SECRET)
                .rawXpath("//input[@name='password']")
                .resolvedXpath("//input[@id='password']")
                .build()
                .toString();

        Assert.assertFalse(text.contains(RAW_SECRET));
        Assert.assertFalse(text.contains(RESOLVED_SECRET));
        Assert.assertTrue(text.contains("keywordName='input'"));
        Assert.assertTrue(text.contains("objectName='txtPassword'"));
        Assert.assertTrue(text.contains("application='BRS'"));
        Assert.assertTrue(text.contains("resolvedXpath='//input[@id='password']'"));
    }

    private ResolvedStepContext resolvedStep() {
        return ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioName("Local keyword execution test")
                .scenarioAction("Local Keyword Test")
                .sheetName("Local Keyword Test")
                .testcaseName("Login BRS")
                .testcaseParentRow(4)
                .excelRow(7)
                .stepNumber(2)
                .keyword("input")
                .objectName("txtPassword")
                .application("BRS")
                .description("Enter password")
                .rawValue(RAW_SECRET)
                .resolvedValue(RESOLVED_SECRET)
                .rawXPath("//input[@name='password']")
                .resolvedXPath("//input[@id='password']")
                .executedBy("com.automation.base.BaseFunction")
                .build();
    }
}
