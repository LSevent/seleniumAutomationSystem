package com.automation.tests;

import com.automation.base.BaseFunction;
import com.automation.context.StepContextHolder;
import com.automation.engine.KeywordResolver;
import com.automation.exceptions.FrameworkException;
import com.automation.models.KeywordExecutionResult;
import com.automation.models.KeywordSourceType;
import com.automation.models.ResolvedStepContext;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

@Test(singleThreaded = true)
public class KeywordResolverNoArgTest {

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

        KeywordExecutionResult result = new KeywordResolver(driver().driver()).execute(
                step.getApplication(),
                step.getKeyword()
        );

        Assert.assertEquals(result.getSourceType(), KeywordSourceType.SPECIFIC);
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

        KeywordExecutionResult result = new KeywordResolver(driver.driver()).execute(
                step.getApplication(),
                step.getKeyword()
        );

        Assert.assertEquals(result.getSourceType(), KeywordSourceType.BASE);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.base.BaseFunction");
        Assert.assertEquals(driver.getCurrentUrl(), step.getResolvedValue());
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void specificNoArgMethodShouldWinWhenBaseHasSameKeyword() {
        FakeWebDriver driver = driver();
        String resolvedXPath = "//button[@id='specific-click']";
        driver.addElement(resolvedXPath, "Login");
        ResolvedStepContext step = step("RESOLVERTEST", "click", "btnLogin", resolvedXPath, "");
        StepContextHolder.set(step);

        KeywordExecutionResult result = new KeywordResolver(driver.driver()).execute(
                step.getApplication(),
                step.getKeyword()
        );

        Assert.assertEquals(result.getSourceType(), KeywordSourceType.SPECIFIC);
        Assert.assertEquals(
                result.getExecutedByClass(),
                "com.automation.functions.RESOLVERTEST.SpecificFunction"
        );
        Assert.assertEquals(
                com.automation.functions.RESOLVERTEST.SpecificFunction.getInvocation(),
                "specific-click"
        );
        Assert.assertTrue(driver.element(resolvedXPath).isClicked());
    }

    @Test
    public void productionSpecificNoArgShouldUseResolvedXPathFromCurrentContext() {
        FakeWebDriver driver = driver();
        String resolvedXPath = "//button[@id='resolved-room']";
        driver.addElement(resolvedXPath, "Meeting Room A");
        ResolvedStepContext step = step(
                "BRS",
                "selectRoomByName",
                "btnRoom",
                resolvedXPath,
                "Meeting Room A"
        );
        StepContextHolder.set(step);

        KeywordExecutionResult result = new KeywordResolver(driver.driver()).execute(
                step.getApplication(),
                step.getKeyword()
        );

        Assert.assertEquals(result.getSourceType(), KeywordSourceType.SPECIFIC);
        Assert.assertTrue(driver.element(resolvedXPath).isClicked());
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void baseNoArgShouldUseResolvedValueFromCurrentContext() {
        FakeWebDriver driver = driver();
        String resolvedXPath = "//input[@id='resolved-username']";
        driver.addElement(resolvedXPath, "");
        ResolvedStepContext step = step("NO_SPECIFIC", "input", "txtUsername", resolvedXPath, "resolved_user");
        StepContextHolder.set(step);

        KeywordExecutionResult result = new KeywordResolver(driver.driver()).execute(
                step.getApplication(),
                step.getKeyword()
        );

        Assert.assertEquals(result.getSourceType(), KeywordSourceType.BASE);
        Assert.assertEquals(driver.element(resolvedXPath).getValue(), "resolved_user");
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void baseFunctionShouldExposeOnlyNoArgKeywordMethods() {
        Set<String> keywords = Set.of(
                "openUrl", "click", "input", "clear", "select", "verifyDisplayed", "verifyNotDisplayed",
                "verifyText", "verifyTextContains", "verifyUrlContains", "verifyTitle",
                "verifyTitleContains", "scrollToElement",
                "pressEnter", "screenshot", "screenshotPartByObject",
                "screenshotFullPart"
        );

        Set<String> actualKeywords = new HashSet<>();

        for (java.lang.reflect.Method method : BaseFunction.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                actualKeywords.add(method.getName());
                Assert.assertEquals(
                        method.getParameterCount(),
                        0,
                        "Public keyword overload should be no-argument: " + method
                );
                Assert.assertEquals(
                        method.getReturnType(),
                        Void.TYPE,
                        "Excel-facing keyword should not return an ignored value: " + method
                );
            }
        }

        Assert.assertEquals(actualKeywords, keywords);
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
                () -> new KeywordResolver(driver().driver()).execute(
                        step.getApplication(),
                        step.getKeyword()
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
        Assert.assertTrue(exception.getMessage().contains("Keyword: approveBooking."));
        Assert.assertTrue(exception.getMessage().contains("Object: btnApproveBooking."));
        Assert.assertTrue(exception.getMessage().contains("Application: BRS."));
        Assert.assertSame(StepContextHolder.get(), step);
    }

    @Test
    public void resolverShouldNeitherInstallNorReplaceStepContext() {
        KeywordResolver resolver = new KeywordResolver(driver().driver());

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
        resolver.execute(step.getApplication(), step.getKeyword());
        Assert.assertSame(StepContextHolder.get(), step);
    }

    private FakeWebDriver driver() {
        return new FakeWebDriver();
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
                .scenarioName("No-arg resolver tests")
                .sheetName("Local Keyword Test")
                .testcaseName("Login BRS")
                .testcaseParentRow(4)
                .excelRow(10)
                .stepNumber(1)
                .keyword(keyword)
                .objectName(objectName)
                .application(application)
                .description("Resolver test step")
                .rawValue(resolvedValue)
                .resolvedValue(resolvedValue)
                .rawXPath(resolvedXPath)
                .resolvedXPath(resolvedXPath)
                .executedBy("")
                .build();
    }
}
