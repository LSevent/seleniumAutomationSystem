package com.automation.tests;

import com.automation.models.ResolvedStepContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ResolvedStepContextBuilderTest {

    private static final String RAW_SECRET = "LOGIN_DATA.PASSWORD_SECRET_TOKEN";
    private static final String RESOLVED_SECRET = "resolved-password-secret-token";

    @Test
    public void builderShouldCreateCompleteImmutableContext() {
        ResolvedStepContext step = completeStep("//button[@id='loginButton']", "Login");

        Assert.assertEquals(step.getScenarioNo(), "1");
        Assert.assertEquals(step.getScenarioAction(), "Local Keyword Test");
        Assert.assertEquals(step.getScenarioName(), "Local Keyword Test");
        Assert.assertEquals(step.getSheetName(), "Local Keyword Test");
        Assert.assertEquals(step.getTestcaseName(), "Login BRS");
        Assert.assertEquals(step.getTestcaseParentRow(), 4);
        Assert.assertEquals(step.getExcelRow(), 7);
        Assert.assertEquals(step.getStepNumber(), 2);
        Assert.assertEquals(step.getKeyword(), "click");
        Assert.assertEquals(step.getObjectName(), "btnLogin");
        Assert.assertEquals(step.getApplication(), "BRS");
        Assert.assertEquals(step.getDescription(), "Click login button");
        Assert.assertEquals(step.getRawValue(), RAW_SECRET);
        Assert.assertEquals(step.getResolvedValue(), "Login");
        Assert.assertEquals(step.getRawXPath(), "//button[@id='loginButton']");
        Assert.assertEquals(step.getResolvedXPath(), "//button[@id='loginButton']");
        Assert.assertEquals(step.getExecutedBy(), "BaseFunction");
    }

    @Test
    public void convenienceMethodsShouldExposeResolvedStepData() {
        ResolvedStepContext step = completeStep("//button[@id='loginButton']", "Login");

        Assert.assertEquals(step.xpath(), "//button[@id='loginButton']");
        Assert.assertEquals(step.value(), "Login");
        Assert.assertEquals(step.rawValue(), RAW_SECRET);
        Assert.assertEquals(step.object(), "btnLogin");
        Assert.assertEquals(step.app(), "BRS");
        Assert.assertEquals(step.objectName(), "btnLogin");
        Assert.assertEquals(step.application(), "BRS");
        Assert.assertTrue(step.hasXPath());
        Assert.assertTrue(step.hasValue());
        Assert.assertTrue(step.hasObject());
        Assert.assertTrue(step.hasApplication());
    }

    @Test
    public void presenceChecksShouldRejectBlankAndNullValues() {
        Assert.assertFalse(ResolvedStepContext.builder().resolvedXPath("").build().hasXPath());
        Assert.assertFalse(ResolvedStepContext.builder().resolvedXPath("   ").build().hasXPath());
        Assert.assertFalse(ResolvedStepContext.builder().resolvedXPath(null).build().hasXPath());
        Assert.assertFalse(ResolvedStepContext.builder().resolvedValue("").build().hasValue());
        Assert.assertFalse(ResolvedStepContext.builder().resolvedValue("   ").build().hasValue());
        Assert.assertFalse(ResolvedStepContext.builder().resolvedValue(null).build().hasValue());
    }

    @Test
    public void toStringShouldExposeContextWithoutSensitiveValues() {
        ResolvedStepContext step = completeStep("//button[@id='loginButton']", RESOLVED_SECRET);

        String text = step.toString();

        Assert.assertFalse(text.contains(RAW_SECRET));
        Assert.assertFalse(text.contains(RESOLVED_SECRET));
        Assert.assertFalse(text.contains("rawValue="));
        Assert.assertFalse(text.contains("resolvedValue="));
        Assert.assertTrue(text.contains("scenarioNo='1'"));
        Assert.assertTrue(text.contains("testcaseName='Login BRS'"));
        Assert.assertTrue(text.contains("keyword='click'"));
        Assert.assertTrue(text.contains("objectName='btnLogin'"));
        Assert.assertTrue(text.contains("application='BRS'"));
        Assert.assertTrue(text.contains("resolvedXPath='//button[@id='loginButton']'"));
    }

    private ResolvedStepContext completeStep(String resolvedXPath, String resolvedValue) {
        return ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioAction("Local Keyword Test")
                .scenarioName("Local Keyword Test")
                .sheetName("Local Keyword Test")
                .testcaseName("Login BRS")
                .testcaseParentRow(4)
                .excelRow(7)
                .stepNumber(2)
                .keyword("click")
                .objectName("btnLogin")
                .application("BRS")
                .description("Click login button")
                .rawValue(RAW_SECRET)
                .resolvedValue(resolvedValue)
                .rawXPath("//button[@id='loginButton']")
                .resolvedXPath(resolvedXPath)
                .executedBy("BaseFunction")
                .build();
    }
}
