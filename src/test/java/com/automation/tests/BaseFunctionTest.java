package com.automation.tests;

import com.automation.base.BaseFunction;
import com.automation.context.StepContextHolder;
import com.automation.models.ResolvedStepContext;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseFunctionTest {

    private static final String USERNAME_XPATH = "//input[@id='username']";
    private static final String BUTTON_XPATH = "//button[@id='loginButton']";
    private static final String MESSAGE_XPATH = "//div[@id='message']";

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        StepContextHolder.clear();
    }

    @Test
    public void shouldExecuteCommonKeywordsWithFakeDriver() {
        FakeWebDriver driver = testDriver();
        BaseFunction baseFunction = new BaseFunction(driver);

        useContext("openUrl", "", "file:///base-function-test.html");
        baseFunction.openUrl();
        useContext("input", USERNAME_XPATH, "brs_admin");
        baseFunction.input();
        useContext("clear", USERNAME_XPATH, "");
        baseFunction.clear();
        useContext("input", USERNAME_XPATH, "brs_user");
        baseFunction.input();
        useContext("click", BUTTON_XPATH, "");
        baseFunction.click();
        useContext("scrollToElement", MESSAGE_XPATH, "");
        baseFunction.scrollToElement();
        useContext("pressEnter", USERNAME_XPATH, "");
        baseFunction.pressEnter();

        Assert.assertEquals(driver.getCurrentUrl(), "file:///base-function-test.html");
        Assert.assertEquals(driver.element(USERNAME_XPATH).getAttribute("value"), "brs_user" + Keys.ENTER);
        Assert.assertTrue(driver.element(BUTTON_XPATH).clicked);
        Assert.assertTrue(driver.scripts.stream().anyMatch(script -> script.contains("scrollIntoView")));
    }

    @Test
    public void shouldVerifyTextUrlTitleAndDisplayState() {
        FakeWebDriver driver = testDriver();
        BaseFunction baseFunction = new BaseFunction(driver);

        useContext("openUrl", "", "file:///base-function-test.html");
        baseFunction.openUrl();

        useContext("getText", MESSAGE_XPATH, "");
        Assert.assertEquals(baseFunction.getText(), "Booking created successfully");
        useContext("isDisplayed", MESSAGE_XPATH, "");
        Assert.assertTrue(baseFunction.isDisplayed());
        useContext("isNotDisplayed", MESSAGE_XPATH, "");
        Assert.assertFalse(baseFunction.isNotDisplayed());
        useContext("waitVisible", MESSAGE_XPATH, "");
        Assert.assertSame(baseFunction.waitVisible(), driver.element(MESSAGE_XPATH));
        useContext("waitClickable", BUTTON_XPATH, "");
        Assert.assertSame(baseFunction.waitClickable(), driver.element(BUTTON_XPATH));

        useContext("verifyDisplayed", MESSAGE_XPATH, "");
        baseFunction.verifyDisplayed();
        useContext("verifyText", MESSAGE_XPATH, "Booking created successfully");
        baseFunction.verifyText();
        useContext("verifyTextContains", MESSAGE_XPATH, "created");
        baseFunction.verifyTextContains();
        useContext("verifyUrlContains", "", "base-function-test");
        baseFunction.verifyUrlContains();
        useContext("verifyTitle", "", "Base Function Test");
        baseFunction.verifyTitle();
        useContext("verifyTitleContains", "", "Function");
        baseFunction.verifyTitleContains();
    }

    @Test
    public void safeClickShouldUseJavaScriptFallbackWhenNormalClickFails() {
        FakeWebDriver driver = testDriver();
        FakeWebElement button = driver.element(BUTTON_XPATH);
        button.failNextClick = true;
        BaseFunction baseFunction = new BaseFunction(driver);

        useContext("safeClick", BUTTON_XPATH, "");
        baseFunction.safeClick();

        Assert.assertTrue(button.clicked);
        Assert.assertTrue(button.javascriptClicked);
    }

    @Test
    public void missingElementShouldThrowClearError() {
        BaseFunction baseFunction = new BaseFunction(testDriver());
        useContext("click", "//button[@id='missing']", "");

        AssertionError error = Assert.expectThrows(
                AssertionError.class,
                baseFunction::click
        );

        Assert.assertTrue(error.getMessage().contains("Element not found for keyword click. XPath: //button[@id='missing']"));
    }

    @Test
    public void verifyTextShouldThrowClearAssertionError() {
        BaseFunction baseFunction = new BaseFunction(testDriver());
        useContext("verifyText", MESSAGE_XPATH, "Booking failed");

        AssertionError error = Assert.expectThrows(
                AssertionError.class,
                baseFunction::verifyText
        );

        Assert.assertTrue(error.getMessage().startsWith(
                "Expected text 'Booking failed' but found 'Booking created successfully'. XPath: //div[@id='message']"
        ));
    }

    private void useContext(String keyword, String resolvedXPath, String resolvedValue) {
        StepContextHolder.set(ResolvedStepContext.builder()
                .scenarioNo("1")
                .scenarioAction("Base Function Test")
                .scenarioName("Base Function Test")
                .sheetName("Base Function Test")
                .testcaseName("Common keywords")
                .testcaseParentRow(2)
                .excelRow(3)
                .stepNumber(1)
                .keyword(keyword)
                .objectName(resolvedXPath == null || resolvedXPath.isBlank() ? "" : "testObject")
                .application("BRS")
                .description("BaseFunction keyword test")
                .rawValue(resolvedValue)
                .resolvedValue(resolvedValue)
                .rawXPath(resolvedXPath)
                .resolvedXPath(resolvedXPath)
                .executedBy("")
                .build());
    }

    private FakeWebDriver testDriver() {
        FakeWebDriver driver = new FakeWebDriver();
        driver.title = "Base Function Test";
        driver.addElement(USERNAME_XPATH, "");
        driver.addElement("//input[@id='password']", "");
        driver.addElement(BUTTON_XPATH, "Login");
        driver.addElement(MESSAGE_XPATH, "Booking created successfully");
        driver.addElement("//h1[@id='pageTitle']", "Dashboard");
        return driver;
    }

    private static class FakeWebDriver implements WebDriver, JavascriptExecutor {

        private final Map<String, FakeWebElement> elements = new HashMap<>();
        private final List<String> scripts = new ArrayList<>();
        private String currentUrl = "";
        private String title = "";

        private FakeWebElement addElement(String xpath, String text) {
            FakeWebElement element = new FakeWebElement(text);
            elements.put(key(By.xpath(xpath)), element);
            return element;
        }

        private FakeWebElement element(String xpath) {
            return elements.get(key(By.xpath(xpath)));
        }

        @Override
        public void get(String url) {
            this.currentUrl = url;
        }

        @Override
        public String getCurrentUrl() {
            return currentUrl;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public List<WebElement> findElements(By by) {
            FakeWebElement element = elements.get(key(by));
            return element == null ? List.of() : List.of(element);
        }

        @Override
        public WebElement findElement(By by) {
            FakeWebElement element = elements.get(key(by));
            if (element == null) {
                throw new NoSuchElementException("Missing element: " + by);
            }
            return element;
        }

        @Override
        public String getPageSource() {
            return "";
        }

        @Override
        public void close() {
        }

        @Override
        public void quit() {
        }

        @Override
        public Set<String> getWindowHandles() {
            return Set.of("fake-window");
        }

        @Override
        public String getWindowHandle() {
            return "fake-window";
        }

        @Override
        public TargetLocator switchTo() {
            throw new UnsupportedOperationException("Fake driver does not support switchTo.");
        }

        @Override
        public Navigation navigate() {
            throw new UnsupportedOperationException("Fake driver does not support navigation.");
        }

        @Override
        public Options manage() {
            throw new UnsupportedOperationException("Fake driver does not support options.");
        }

        @Override
        public Object executeScript(String script, Object... args) {
            scripts.add(script);
            if (script.contains("click") && args.length > 0 && args[0] instanceof FakeWebElement element) {
                element.clicked = true;
                element.javascriptClicked = true;
            }
            return null;
        }

        @Override
        public Object executeAsyncScript(String script, Object... args) {
            scripts.add(script);
            return null;
        }

        private String key(By by) {
            return by.toString();
        }
    }

    private static class FakeWebElement implements WebElement {

        private final String text;
        private String value = "";
        private String lastKeys = "";
        private boolean displayed = true;
        private boolean enabled = true;
        private boolean clicked;
        private boolean javascriptClicked;
        private boolean failNextClick;

        private FakeWebElement(String text) {
            this.text = text;
        }

        @Override
        public void click() {
            if (failNextClick) {
                failNextClick = false;
                throw new ElementClickInterceptedException("Fake click interception.");
            }
            clicked = true;
        }

        @Override
        public void submit() {
        }

        @Override
        public void sendKeys(CharSequence... keysToSend) {
            StringBuilder builder = new StringBuilder();
            for (CharSequence keys : keysToSend) {
                builder.append(keys);
            }
            lastKeys = builder.toString();
            value += lastKeys;
        }

        @Override
        public void clear() {
            value = "";
        }

        @Override
        public String getTagName() {
            return "input";
        }

        @Override
        public String getAttribute(String name) {
            return "value".equals(name) ? value : null;
        }

        @Override
        public String getDomAttribute(String name) {
            return getAttribute(name);
        }

        @Override
        public String getDomProperty(String name) {
            return getAttribute(name);
        }

        @Override
        public String getAriaRole() {
            return "";
        }

        @Override
        public String getAccessibleName() {
            return "";
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public List<WebElement> findElements(By by) {
            return List.of();
        }

        @Override
        public WebElement findElement(By by) {
            throw new NoSuchElementException("Fake nested element missing: " + by);
        }

        @Override
        public SearchContext getShadowRoot() {
            throw new UnsupportedOperationException("Fake element does not support shadow root.");
        }

        @Override
        public boolean isDisplayed() {
            return displayed;
        }

        @Override
        public Point getLocation() {
            return new Point(0, 0);
        }

        @Override
        public Dimension getSize() {
            return new Dimension(100, 20);
        }

        @Override
        public Rectangle getRect() {
            return new Rectangle(getPoint(), getDimension());
        }

        @Override
        public String getCssValue(String propertyName) {
            return "";
        }

        @Override
        public <X> X getScreenshotAs(OutputType<X> target) {
            return null;
        }

        private Point getPoint() {
            return new Point(0, 0);
        }

        private Dimension getDimension() {
            return new Dimension(100, 20);
        }
    }
}
