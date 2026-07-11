package com.automation.tests;

import com.automation.models.TestStep;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestStepBuilderTest {

    private static final String SENSITIVE_VALUE = "superSecretPassword123";

    @Test
    public void builderShouldCreateTestStepWithAllFields() {
        TestStep step = TestStep.builder()
                .scenarioNo("1")
                .scenarioName("Local keyword execution test")
                .scenarioAction("Local Keyword Test")
                .testcaseName("Login BRS")
                .keyword("input")
                .object("txtPassword")
                .value(SENSITIVE_VALUE)
                .application("BRS")
                .description("Enter password")
                .run(false)
                .excelRowNumber(7)
                .stepOrder(2)
                .build();

        Assert.assertEquals(step.getScenarioNo(), "1");
        Assert.assertEquals(step.getScenarioName(), "Local keyword execution test");
        Assert.assertEquals(step.getScenarioAction(), "Local Keyword Test");
        Assert.assertEquals(step.getTestcaseName(), "Login BRS");
        Assert.assertEquals(step.getKeyword(), "input");
        Assert.assertEquals(step.getObject(), "txtPassword");
        Assert.assertEquals(step.getValue(), SENSITIVE_VALUE);
        Assert.assertEquals(step.getApplication(), "BRS");
        Assert.assertEquals(step.getDescription(), "Enter password");
        Assert.assertFalse(step.isRun());
        Assert.assertEquals(step.getExcelRowNumber(), 7);
        Assert.assertEquals(step.getStepOrder(), 2);
    }

    @Test
    public void rowNumberAliasShouldMapToExcelRowNumber() {
        TestStep step = TestStep.builder()
                .keyword("click")
                .rowNumber(8)
                .stepOrder(3)
                .build();

        Assert.assertEquals(step.getKeyword(), "click");
        Assert.assertEquals(step.getExcelRowNumber(), 8);
        Assert.assertEquals(step.getStepOrder(), 3);
        Assert.assertTrue(step.isRun());
    }

    @Test
    public void toStringShouldRemainSensitiveDataSafe() {
        String text = TestStep.builder()
                .testcaseName("Login BRS")
                .keyword("input")
                .object("txtPassword")
                .value(SENSITIVE_VALUE)
                .application("BRS")
                .description("Enter password")
                .rowNumber(7)
                .stepOrder(2)
                .build()
                .toString();

        Assert.assertFalse(text.contains(SENSITIVE_VALUE));
        Assert.assertTrue(text.contains("keyword='input'"));
        Assert.assertTrue(text.contains("object='txtPassword'"));
        Assert.assertTrue(text.contains("application='BRS'"));
        Assert.assertTrue(text.contains("excelRowNumber=7"));
    }
}
