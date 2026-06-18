package com.automation.tests;

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

@Test(singleThreaded = true)
public class FunctionResolverNoArgTest {

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        StepContextHolder.clear();
        com.automation.functions.RESOLVERTEST.SpecificFunction.reset();
    }

    @Test
    public void specificNoArgMethodShouldBePreferredOverParameterMethod() {
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
                step.getFunction(),
                "//button[@id='wrong']",
                "wrong-value"
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
        ResolvedStepContext step = step("NO_SPECIFIC", "toString", "", "", "");
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver().driver()).execute(
                step.getApplication(),
                step.getFunction(),
                "",
                ""
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.BASE);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.base.BaseFunction");
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void specificNoArgMethodShouldWinWhenBaseHasSameKeyword() {
        ResolvedStepContext step = step("RESOLVERTEST", "toString", "", "", "");
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver().driver()).execute(
                step.getApplication(),
                step.getFunction(),
                "",
                ""
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertEquals(
                result.getExecutedByClass(),
                "com.automation.functions.RESOLVERTEST.SpecificFunction"
        );
        Assert.assertEquals(
                com.automation.functions.RESOLVERTEST.SpecificFunction.getInvocation(),
                "specific-toString"
        );
    }

    @Test
    public void parameterFallbackShouldUseResolvedXpathFromCurrentContext() {
        FakeWebDriver driver = driver();
        String resolvedXpath = "//button[@id='resolved-login']";
        driver.addElement(resolvedXpath, "Login");
        ResolvedStepContext step = step("BRS", "click", "btnLogin", resolvedXpath, "");
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver.driver()).execute(
                step.getApplication(),
                step.getFunction(),
                "//button[@id='wrong-login']",
                "wrong-value"
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertTrue(driver.element(resolvedXpath).isClicked());
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void parameterFallbackShouldUseResolvedValueFromCurrentContext() {
        FakeWebDriver driver = driver();
        String resolvedXpath = "//input[@id='resolved-username']";
        driver.addElement(resolvedXpath, "");
        ResolvedStepContext step = step("NO_SPECIFIC", "input", "txtUsername", resolvedXpath, "resolved_user");
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver.driver()).execute(
                step.getApplication(),
                step.getFunction(),
                "//input[@id='wrong-username']",
                "wrong_user"
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.BASE);
        Assert.assertEquals(driver.element(resolvedXpath).getValue(), "resolved_user");
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void contextFallbackShouldReceiveXpathValueAndResolvedContext() {
        ResolvedStepContext step = step(
                "RESOLVERTEST",
                "legacyWithContext",
                "btnContext",
                "//button[@id='resolved-context']",
                "resolved-context-value"
        );
        StepContextHolder.set(step);

        FunctionExecutionResult result = new FunctionResolver(driver().driver()).execute(
                step.getApplication(),
                step.getFunction(),
                "//button[@id='wrong-context']",
                "wrong-context-value"
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertEquals(
                com.automation.functions.RESOLVERTEST.SpecificFunction.getReceivedXpath(),
                step.getResolvedXPath()
        );
        Assert.assertEquals(
                com.automation.functions.RESOLVERTEST.SpecificFunction.getReceivedValue(),
                step.getResolvedValue()
        );
        Assert.assertSame(
                com.automation.functions.RESOLVERTEST.SpecificFunction.getReceivedContext(),
                step
        );
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
                        step.getFunction(),
                        step.getResolvedXPath(),
                        step.getResolvedValue()
                )
        );

        Assert.assertTrue(exception.getMessage().contains(
                "Keyword 'approveBooking' not found in SpecificFunction for application 'BRS' or BaseFunction."
        ));
        Assert.assertTrue(exception.getMessage().contains("Scenario NO: 1."));
        Assert.assertTrue(exception.getMessage().contains("Scenario ACTION: Local Keyword Test."));
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
        resolver.execute("NO_SPECIFIC", "toString", "", "");
        Assert.assertTrue(StepContextHolder.current().isEmpty());

        ResolvedStepContext step = step("NO_SPECIFIC", "toString", "", "", "");
        StepContextHolder.set(step);
        resolver.execute(step.getApplication(), step.getFunction(), "", "");
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
