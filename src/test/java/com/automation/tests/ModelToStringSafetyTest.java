package com.automation.tests;

import com.automation.models.ExecutionResult;
import com.automation.models.ResolvedObject;
import com.automation.models.ResolvedStepContext;
import com.automation.models.TestStep;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ModelToStringSafetyTest {

    private static final String RAW_SECRET = "superSecretPassword123";
    private static final String TOKEN_SECRET = "token-abc-123";
    private static final String RESOLVED_SECRET = "sensitiveResolvedValue";

    @Test
    public void resolvedStepContextToStringShouldNotExposeRawOrResolvedValues() {
        String text = resolvedStep().toString();

        assertDoesNotExposeSecrets(text);
        Assert.assertTrue(text.contains("scenarioNo='1'"));
        Assert.assertTrue(text.contains("keyword='input'"));
        Assert.assertTrue(text.contains("objectName='txtPassword'"));
        Assert.assertTrue(text.contains("application='BRS'"));
        Assert.assertTrue(text.contains("resolvedXPath='//input[@id='password']'"));
    }

    @Test
    public void testStepToStringShouldNotExposeValue() {
        TestStep step = new TestStep(
                "1",
                "Local Keyword Test",
                "Local Keyword Test",
                "Login BRS",
                "input",
                "txtPassword",
                RAW_SECRET,
                "BRS",
                "Enter password",
                7,
                2
        );

        String text = step.toString();

        Assert.assertFalse(text.contains(RAW_SECRET));
        Assert.assertTrue(text.contains("keyword='input'"));
        Assert.assertTrue(text.contains("object='txtPassword'"));
        Assert.assertTrue(text.contains("application='BRS'"));
        Assert.assertTrue(text.contains("excelRowNumber=7"));
    }

    @Test
    public void executionResultToStringShouldNotExposeRawOrResolvedValues() {
        ExecutionResult result = ExecutionResult.success(
                resolvedStep(),
                "BaseFunction",
                "KEYWORD",
                "Step passed"
        );

        String text = result.toString();

        assertDoesNotExposeSecrets(text);
        Assert.assertTrue(text.contains("status='PASS'"));
        Assert.assertTrue(text.contains("keywordName='input'"));
        Assert.assertTrue(text.contains("objectName='txtPassword'"));
        Assert.assertTrue(text.contains("application='BRS'"));
        Assert.assertTrue(text.contains("resolvedXPath='//input[@id='password']'"));
    }

    @Test
    public void resolvedObjectToStringShouldNotExposeRawOrResolvedValues() {
        ResolvedObject object = new ResolvedObject(
                "txtPassword",
                "BRS",
                "//input[@name='password-${secret}']",
                "//input[@id='password']",
                TOKEN_SECRET,
                RESOLVED_SECRET,
                12
        );

        String text = object.toString();

        assertDoesNotExposeSecrets(text);
        Assert.assertTrue(text.contains("originalObjectName='txtPassword'"));
        Assert.assertTrue(text.contains("application='BRS'"));
        Assert.assertTrue(text.contains("resolvedXPath='//input[@id='password']'"));
        Assert.assertTrue(text.contains("excelRowNumber=12"));
    }

    private ResolvedStepContext resolvedStep() {
        return ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioAction("Local Keyword Test")
                .scenarioName("Local Keyword Test")
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
                .rawXPath("//input[@name='password-${secret}']")
                .resolvedXPath("//input[@id='password']")
                .executedBy("BaseFunction")
                .build();
    }

    private void assertDoesNotExposeSecrets(String text) {
        Assert.assertFalse(text.contains(RAW_SECRET));
        Assert.assertFalse(text.contains(TOKEN_SECRET));
        Assert.assertFalse(text.contains(RESOLVED_SECRET));
    }
}
