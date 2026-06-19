package com.automation.tests;

import com.automation.base.BaseFunction;
import com.automation.context.StepContextHolder;
import com.automation.engine.FunctionResolver;
import com.automation.exceptions.FrameworkException;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.FunctionSourceType;
import com.automation.models.ResolvedStepContext;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Modifier;
import java.util.Set;

@Test(singleThreaded = true)
public class FunctionResolverNoArgTest {

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        StepContextHolder.clear();
        com.automation.functions.RESOLVERTEST.SpecificFunction.reset();
    }

    @Test
    public void specificNoArgMethodShouldExecuteWithCurrentContext() {
        ResolvedStepContext step = step(
                "RESOLVERTEST",
                "preferNoArg",
                "btnPreferred",
                "//button[@id='preferred']",
                "preferred-value"
        );
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver().driver()).execute(
                step.getApplication(),
                step.getFunction()
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertEquals(
                com.automation.functions.RESOLVERTEST.SpecificFunction.getInvocation(),
                "no-arg"
        );
        Assert.assertSame(
                com.automation.functions.RESOLVERTEST.SpecificFunction.getReceivedContext(),
                step
        );
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void baseNoArgMethodShouldExecute() {
        FakeWebDriver driver = driver();
        ResolvedStepContext step = step(
                "NO_SPECIFIC",
                "openUrl",
                "",
                "",
                "file:///no-arg-base-test.html"
        );
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver.driver()).execute(
                step.getApplication(),
                step.getFunction()
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.BASE);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.base.BaseFunction");
        Assert.assertEquals(driver.getCurrentUrl(), step.getResolvedValue());
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void specificNoArgMethodShouldWinWhenBaseHasSameKeyword() {
        FakeWebDriver driver = driver();
        String resolvedXpath = "//button[@id='specific-click']";
        driver.addElement(resolvedXpath, "Login");
        ResolvedStepContext step = step("BRS", "click", "btnLogin", resolvedXpath, "");
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver.driver()).execute(
                step.getApplication(),
                step.getFunction()
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertEquals(
                result.getExecutedByClass(),
                "com.automation.functions.BRS.SpecificFunction"
        );
        Assert.assertTrue(driver.element(resolvedXpath).isClicked());
    }

    @Test
    public void productionSpecificNoArgShouldUseResolvedXpathFromCurrentContext() {
        FakeWebDriver driver = driver();
        String resolvedXpath = "//button[@id='resolved-room']";
        driver.addElement(resolvedXpath, "Meeting Room A");
        ResolvedStepContext step = step(
                "BRS",
                "selectRoomByName",
                "btnRoom",
                resolvedXpath,
                "Meeting Room A"
        );
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver.driver()).execute(
                step.getApplication(),
                step.getFunction()
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertTrue(driver.element(resolvedXpath).isClicked());
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void baseNoArgShouldUseResolvedValueFromCurrentContext() {
        FakeWebDriver driver = driver();
        String resolvedXpath = "//input[@id='resolved-username']";
        driver.addElement(resolvedXpath, "");
        ResolvedStepContext step = step("NO_SPECIFIC", "input", "txtUsername", resolvedXpath, "resolved_user");
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver.driver()).execute(
                step.getApplication(),
                step.getFunction()
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.BASE);
        Assert.assertEquals(driver.element(resolvedXpath).getValue(), "resolved_user");
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void baseFunctionShouldExposeOnlyNoArgKeywordMethods() {
        Set<String> keywords = Set.of(
                "openUrl", "click", "input", "clear", "getText", "verifyDisplayed",
                "verifyText", "verifyTextContains", "verifyUrlContains", "verifyTitle",
                "verifyTitleContains", "waitVisible", "waitClickable", "scrollToElement",
                "safeClick", "pressEnter", "isDisplayed", "isNotDisplayed"
        );

        for (java.lang.reflect.Method method : BaseFunction.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && keywords.contains(method.getName())) {
                Assert.assertEquals(
                        method.getParameterCount(),
                        0,
                        "Public keyword overload should be no-argument: " + method
                );
            }
        }
    }

    @Test
    public void unknownKeywordShouldIncludeCompleteCurrentContext() {
        ResolvedStepContext step = step(
                "BRS",
                "approveBooking",
                "btnApproveBooking",
                "//button[@id='approve']",
                ""
        );
        StepContextHolder.set(step);

        FrameworkException exception = Assert.expectThrows(
                FrameworkException.class,
                () -> new FunctionResolver(driver().driver()).execute(
                        step.getApplication(),
                        step.getFunction()
                )
        );

        Assert.assertTrue(exception.getMessage().contains(
                "Keyword 'approveBooking' not found in SpecificFunction for application 'BRS' or BaseFunction."
        ));
        Assert.assertTrue(exception.getMessage().contains("Scenario NO: 1."));
        Assert.assertTrue(exception.getMessage().contains("Scenario ACTION: Local Keyword Test."));
        Assert.assertTrue(exception.getMessage().contains("Sheet: Local Keyword Test."));
        Assert.assertTrue(exception.getMessage().contains("Testcase: Login BRS."));
        Assert.assertTrue(exception.getMessage().contains("Row: 10."));
        Assert.assertTrue(exception.getMessage().contains("Function: approveBooking."));
        Assert.assertTrue(exception.getMessage().contains("Object: btnApproveBooking."));
        Assert.assertTrue(exception.getMessage().contains("Application: BRS."));
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void resolverShouldNeitherInstallNorReplaceStepContext() {
        FunctionResolver resolver = new FunctionResolver(driver().driver());

        Assert.assertTrue(StepContextHolder.current().isEmpty());
        FrameworkException missingContext = Assert.expectThrows(
                FrameworkException.class,
                () -> resolver.execute("NO_SPECIFIC", "openUrl")
        );
        Assert.assertEquals(
                missingContext.getMessage(),
                "Step context is not available. Keyword must be executed through KeywordEngine."
        );
        Assert.assertTrue(StepContextHolder.current().isEmpty());

        ResolvedStepContext step = step("NO_SPECIFIC", "openUrl", "", "", "file:///resolver-test.html");
        StepContextHolder.set(step);
        resolver.execute(step.getApplication(), step.getFunction());
        Assert.assertSame(StepContextHolder.get(), step);
    }

    private FakeWebDriver driver() {
        return new FakeWebDriver();
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
                "No-arg resolver tests",
                "Local Keyword Test",
                "Login BRS",
                4,
                10,
                1,
                function,
                objectName,
                application,
                "Resolver test step",
                resolvedValue,
                resolvedValue,
                resolvedXpath,
                resolvedXpath,
                ""
        );
    }
}
