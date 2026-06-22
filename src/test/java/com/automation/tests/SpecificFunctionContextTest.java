package com.automation.tests;

import com.automation.context.StepContextHolder;
import com.automation.engine.FunctionResolver;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.FunctionSourceType;
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
    public void brsNoArgClickShouldWinOverBaseAndUseResolvedXpath() {
        FakeWebDriver driver = new FakeWebDriver();
        String resolvedXpath = "//button[@id='resolved-login']";
        driver.addElement(resolvedXpath, "Login");
        ResolvedStepContext step = step("BRS", "click", "btnLogin", resolvedXpath, "");
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver.driver()).execute(
                step.getApplication(),
                step.getFunction()
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.functions.BRS.SpecificFunction");
        Assert.assertTrue(driver.element(resolvedXpath).isClicked());
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void hrisNoArgKeywordShouldReadResolvedXpathAndValue() {
        FakeWebDriver driver = new FakeWebDriver();
        String resolvedXpath = "//div[@id='resolved-employee']";
        driver.addElement(resolvedXpath, "Employee Alice is active");
        ResolvedStepContext step = step(
                "HRIS",
                "verifyEmployeeVisible",
                "employeeCard",
                resolvedXpath,
                "Alice"
        );
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver.driver()).execute(
                step.getApplication(),
                step.getFunction()
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.functions.HRIS.SpecificFunction");
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void productionSpecificKeywordsShouldExposeOnlyNoArgEntryPoints() {
        assertNoArgKeyword(com.automation.functions.BRS.SpecificFunction.class, "click");
        assertNoArgKeyword(com.automation.functions.BRS.SpecificFunction.class, "selectRoomByName");
        assertNoArgKeyword(com.automation.functions.BRS.SpecificFunction.class, "verifyBookingCreated");
        assertNoArgKeyword(com.automation.functions.HRIS.SpecificFunction.class, "verifyEmployeeVisible");
    }

    private ResolvedStepContext step(
            String application,
            String function,
            String objectName,
            String resolvedXpath,
            String resolvedValue
    ) {
        return new ResolvedStepContext(
                "1",
                "Local Keyword Test",
                "SpecificFunction context tests",
                "Local Keyword Test",
                "Login BRS",
                4,
                10,
                1,
                function,
                objectName,
                application,
                "Application-specific keyword",
                resolvedValue,
                resolvedValue,
                resolvedXpath,
                resolvedXpath,
                ""
        );
    }

    private void assertNoArgKeyword(Class<?> functionClass, String keyword) {
        java.lang.reflect.Method[] methods = java.util.Arrays.stream(functionClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(keyword))
                .toArray(java.lang.reflect.Method[]::new);
        Assert.assertEquals(methods.length, 1, "Expected one public keyword entry point for " + keyword);
        Assert.assertEquals(methods[0].getParameterCount(), 0);
    }
}
