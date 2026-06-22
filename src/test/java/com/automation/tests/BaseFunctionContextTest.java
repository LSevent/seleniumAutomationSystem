package com.automation.tests;

import com.automation.base.BaseFunction;
import com.automation.context.StepContextHolder;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ResolvedStepContext;
import com.automation.tests.support.FakeWebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class BaseFunctionContextTest {

    @AfterMethod(alwaysRun = true)
    public void clearContext() {
        StepContextHolder.clear();
    }

    @Test
    public void openUrlShouldUseResolvedValueFromCurrentContext() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step("openUrl", "", "CONFIG.BASE_URL", "file:///resolved.html", "", ""));

        new BaseFunction(driver.driver()).openUrl();

        Assert.assertEquals(driver.getCurrentUrl(), "file:///resolved.html");
    }

    @Test
    public void clickShouldUseResolvedXpathInsteadOfObjectNameOrRawXpath() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step(
                "click",
                "not-an-xpath",
                "",
                "",
                "//button[@id='raw-login']",
                "//button[@id='resolved-login']"
        ));

        new BaseFunction(driver.driver()).click();

        Assert.assertTrue(driver.element("//button[@id='resolved-login']").isClicked());
    }

    @Test
    public void inputShouldUseResolvedXpathAndResolvedValue() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step(
                "input",
                "txtUsername",
                "LOGIN_DATA.USERNAME",
                "resolved_user",
                "//input[@id='raw-username']",
                "//input[@id='resolved-username']"
        ));

        new BaseFunction(driver.driver()).input();

        Assert.assertEquals(driver.element("//input[@id='resolved-username']").getValue(), "resolved_user");
    }

    @Test
    public void verifyDisplayedShouldUseResolvedXpath() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step(
                "verifyDisplayed",
                "lblDashboard",
                "",
                "",
                "//h1[@id='raw-dashboard']",
                "//h1[@id='resolved-dashboard']"
        ));

        new BaseFunction(driver.driver()).verifyDisplayed();
    }

    @Test
    public void verifyTextShouldUseResolvedXpathAndResolvedValue() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step(
                "verifyText",
                "lblMessage",
                "LOGIN_DATA.MESSAGE",
                "Ready",
                "//div[@id='raw-message']",
                "//div[@id='resolved-message']"
        ));

        new BaseFunction(driver.driver()).verifyText();
    }

    @Test
    public void sharedContextHelpersShouldReadFromStepContextHolder() {
        FakeWebDriver driver = driver();
        StepContextHolder.set(step(
                "customKeyword",
                "customObject",
                "RAW_VALUE",
                "resolved value",
                "//raw",
                "//resolved"
        ));
        ContextHelperProbe function = new ContextHelperProbe(driver.driver());

        Assert.assertEquals(function.xpath("customKeyword"), "//resolved");
        Assert.assertEquals(function.value("customKeyword", "Value"), "resolved value");
    }

    @Test
    public void clickShouldFailClearlyWhenContextIsMissing() {
        FrameworkException exception = Assert.expectThrows(
                FrameworkException.class,
                () -> new BaseFunction(driver().driver()).click()
        );

        Assert.assertEquals(
                exception.getMessage(),
                "Step context is not available. Keyword must be executed through KeywordEngine."
        );
    }

    @Test
    public void clickShouldFailClearlyWhenResolvedXpathIsBlank() {
        StepContextHolder.set(step("click", "btnLogin", "", "", "//button[@id='raw-login']", ""));

        FrameworkException exception = Assert.expectThrows(
                FrameworkException.class,
                () -> new BaseFunction(driver().driver()).click()
        );

        assertContextualValidation(exception, "XPath is required for keyword 'click'.", "btnLogin");
    }

    @Test
    public void inputShouldFailClearlyWhenResolvedValueIsBlank() {
        StepContextHolder.set(step(
                "input",
                "txtUsername",
                "LOGIN_DATA.USERNAME",
                "",
                "//input[@id='raw-username']",
                "//input[@id='resolved-username']"
        ));

        FrameworkException exception = Assert.expectThrows(
                FrameworkException.class,
                () -> new BaseFunction(driver().driver()).input()
        );

        assertContextualValidation(exception, "Value is required for keyword 'input'.", "txtUsername");
    }

    @Test
    public void openUrlShouldFailClearlyWhenResolvedValueIsBlank() {
        StepContextHolder.set(step("openUrl", "", "CONFIG.BASE_URL", "", "", ""));

        FrameworkException exception = Assert.expectThrows(
                FrameworkException.class,
                () -> new BaseFunction(driver().driver()).openUrl()
        );

        assertContextualValidation(exception, "URL is required for keyword 'openUrl'.", "");
    }

    @Test
    public void verifyTextShouldFailClearlyWhenResolvedValueIsBlank() {
        StepContextHolder.set(step(
                "verifyText",
                "lblMessage",
                "LOGIN_DATA.MESSAGE",
                "",
                "//div[@id='raw-message']",
                "//div[@id='resolved-message']"
        ));

        FrameworkException exception = Assert.expectThrows(
                FrameworkException.class,
                () -> new BaseFunction(driver().driver()).verifyText()
        );

        assertContextualValidation(exception, "Expected text is required for keyword 'verifyText'.", "lblMessage");
    }

    private void assertContextualValidation(
            FrameworkException exception,
            String summary,
            String objectName
    ) {
        Assert.assertTrue(exception.getMessage().startsWith(summary));
        Assert.assertTrue(exception.getMessage().contains("Scenario NO: 1."));
        Assert.assertTrue(exception.getMessage().contains("Scenario ACTION: Local Keyword Test."));
        Assert.assertTrue(exception.getMessage().contains("Sheet: Local Keyword Test."));
        Assert.assertTrue(exception.getMessage().contains("Testcase: Login BRS."));
        Assert.assertTrue(exception.getMessage().contains("Row: 7."));
        if (!objectName.isBlank()) {
            Assert.assertTrue(exception.getMessage().contains("Object: " + objectName + "."));
        }
        Assert.assertTrue(exception.getMessage().contains("Application: BRS."));
    }

    private FakeWebDriver driver() {
        FakeWebDriver driver = new FakeWebDriver();
        driver.addElement("//button[@id='resolved-login']", "Login");
        driver.addElement("//input[@id='resolved-username']", "");
        driver.addElement("//input[@id='resolved-password']", "");
        driver.addElement("//h1[@id='resolved-dashboard']", "Dashboard");
        driver.addElement("//div[@id='resolved-message']", "Ready");
        return driver;
    }

    private ResolvedStepContext step(
            String function,
            String objectName,
            String rawValue,
            String resolvedValue,
            String rawXpath,
            String resolvedXpath
    ) {
        return new ResolvedStepContext(
                "1",
                "Local Keyword Test",
                "Context-based BaseFunction keywords",
                "Local Keyword Test",
                "Login BRS",
                4,
                7,
                1,
                function,
                objectName,
                "BRS",
                "BaseFunction context test",
                rawValue,
                resolvedValue,
                rawXpath,
                resolvedXpath,
                ""
        );
    }

    private static final class ContextHelperProbe extends BaseFunction {

        private ContextHelperProbe(org.openqa.selenium.WebDriver driver) {
            super(driver);
        }

        private String xpath(String keyword) {
            return requiredXPath(keyword);
        }

        private String value(String keyword, String label) {
            return requiredValue(keyword, label);
        }
    }

}
