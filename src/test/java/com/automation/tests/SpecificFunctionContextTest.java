package com.automation.tests;

import com.automation.context.StepContextHolder;
import com.automation.engine.KeywordResolver;
import com.automation.models.KeywordExecutionResult;
import com.automation.models.KeywordSourceType;
import com.automation.models.ResolvedStepContext;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class SpecificFunctionContextTest {

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        StepContextHolder.clear();
    }

    @Test
    public void brsClickShouldUseReusableBaseKeyword() {
        FakeWebDriver driver = new FakeWebDriver();
        String resolvedXPath = "//button[@id='resolved-login']";
        driver.addElement(resolvedXPath, "Login");
        ResolvedStepContext step = step("BRS", "click", "btnLogin", resolvedXPath, "");
        StepContextHolder.set(step);

        KeywordExecutionResult result = new KeywordResolver(driver.driver()).execute(
                step.getApplication(),
                step.getKeyword()
        );

        Assert.assertEquals(result.getSourceType(), KeywordSourceType.BASE);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.base.BaseFunction");
        Assert.assertTrue(driver.element(resolvedXPath).isClicked());
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void hrisNoArgKeywordShouldReadResolvedXPathAndValue() {
        FakeWebDriver driver = new FakeWebDriver();
        String resolvedXPath = "//div[@id='resolved-employee']";
        driver.addElement(resolvedXPath, "Employee Alice is active");
        ResolvedStepContext step = step(
                "HRIS",
                "verifyEmployeeVisible",
                "employeeCard",
                resolvedXPath,
                "Alice"
        );
        StepContextHolder.set(step);

        KeywordExecutionResult result = new KeywordResolver(driver.driver()).execute(
                step.getApplication(),
                step.getKeyword()
        );

        Assert.assertEquals(result.getSourceType(), KeywordSourceType.SPECIFIC);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.functions.HRIS.SpecificFunction");
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void productionSpecificKeywordsShouldExposeOnlyNoArgEntryPoints() {
        assertNoArgKeyword(com.automation.functions.BRS.SpecificFunction.class, "clickMultiValue");
        assertNoArgKeyword(com.automation.functions.HRIS.SpecificFunction.class, "verifyEmployeeVisible");
    }

    private ResolvedStepContext step(
            String application,
            String keyword,
            String objectName,
            String resolvedXPath,
            String resolvedValue
    ) {
        return ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioAction("Local Keyword Test")
                .scenarioName("SpecificFunction context tests")
                .sheetName("Local Keyword Test")
                .testcaseName("Login BRS")
                .testcaseParentRow(4)
                .excelRow(10)
                .stepNumber(1)
                .keyword(keyword)
                .objectName(objectName)
                .application(application)
                .description("Application-specific keyword")
                .rawValue(resolvedValue)
                .resolvedValue(resolvedValue)
                .rawXPath(resolvedXPath)
                .resolvedXPath(resolvedXPath)
                .executedBy("")
                .build();
    }

    private void assertNoArgKeyword(Class<?> functionClass, String keyword) {
        java.lang.reflect.Method[] methods = java.util.Arrays.stream(functionClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(keyword))
                .toArray(java.lang.reflect.Method[]::new);
        Assert.assertEquals(methods.length, 1, "Expected one public keyword entry point for " + keyword);
        Assert.assertEquals(methods[0].getParameterCount(), 0);
    }
}
