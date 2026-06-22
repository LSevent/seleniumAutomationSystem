package com.automation.tests;

import com.automation.context.StepContextHolder;
import com.automation.engine.FunctionResolver;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.FunctionSourceType;
import com.automation.models.ResolvedFunction;
import com.automation.models.ResolvedStepContext;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FunctionResolverTest {

    private static final String USERNAME_XPATH = "//input[@id='username']";
    private static final String LOGIN_BUTTON_XPATH = "//button[@id='loginButton']";
    private static final String ROOM_BUTTON_XPATH = "//button[@id='roomA']";
    private static final String MESSAGE_XPATH = "//div[@id='message']";
    private static final String PAGE_TITLE_XPATH = "//h1[@id='pageTitle']";

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        StepContextHolder.clear();
    }

    @Test
    public void shouldResolveUppercaseSpecificFunctionClassForKnownApplications() {
        FunctionResolver resolver = new FunctionResolver(testDriver().driver());

        assertSpecificClass(resolver.resolve("brs", "waitForApplicationReady"), "BRS");
        assertSpecificClass(resolver.resolve("Brs", "waitForApplicationReady"), "BRS");
        assertSpecificClass(resolver.resolve("HRIS", "waitForApplicationReady"), "HRIS");
        assertSpecificClass(resolver.resolve("crm", "waitForApplicationReady"), "CRM");
    }

    @Test
    public void shouldFallbackToBaseFunctionWhenSpecificFunctionDoesNotOverrideKeyword() {
        FakeDriver fakeDriver = testDriver();
        FunctionResolver resolver = new FunctionResolver(fakeDriver.driver());

        FunctionExecutionResult result = execute(resolver, "BRS", "input", USERNAME_XPATH, "brs_user");

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.BASE);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.base.BaseFunction");
        Assert.assertEquals(fakeDriver.element(USERNAME_XPATH).value, "brs_user");
    }

    @Test
    public void shouldPrioritizeSpecificFunctionWhenKeywordExistsInSpecificAndBase() {
        FakeDriver fakeDriver = testDriver();
        FunctionResolver resolver = new FunctionResolver(fakeDriver.driver());

        FunctionExecutionResult result = execute(resolver, "BRS", "click", LOGIN_BUTTON_XPATH, "");

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.functions.BRS.SpecificFunction");
        Assert.assertTrue(fakeDriver.element(LOGIN_BUTTON_XPATH).clicked);
    }

    @Test
    public void shouldExecuteBrsSpecificKeyword() {
        FakeDriver fakeDriver = testDriver();
        FunctionResolver resolver = new FunctionResolver(fakeDriver.driver());

        FunctionExecutionResult result = execute(
                resolver,
                "BRS",
                "selectRoomByName",
                ROOM_BUTTON_XPATH,
                "Meeting Room A"
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertTrue(fakeDriver.element(ROOM_BUTTON_XPATH).clicked);
    }

    @Test
    public void shouldExecuteBaseFunctionVerifyTextThroughResolver() {
        FunctionResolver resolver = new FunctionResolver(testDriver().driver());

        FunctionExecutionResult result = execute(
                resolver,
                "BRS",
                "verifyText",
                MESSAGE_XPATH,
                "Booking created successfully"
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.BASE);
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void shouldExecuteValueOnlyKeywordThroughResolver() {
        FunctionResolver resolver = new FunctionResolver(testDriver().driver());

        FunctionExecutionResult result = execute(resolver, "BRS", "verifyTitleContains", "", "Dashboard");

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.BASE);
        Assert.assertTrue(result.isSuccess());
    }

    @Test
    public void shouldFallbackToBaseFunctionForUnknownApplicationWhenKeywordExists() {
        FunctionResolver resolver = new FunctionResolver(testDriver().driver());

        FunctionExecutionResult result = execute(
                resolver,
                "UNKNOWN",
                "verifyText",
                MESSAGE_XPATH,
                "Booking created successfully"
        );

        Assert.assertEquals(result.getSourceType(), FunctionSourceType.BASE);
        Assert.assertEquals(result.getExecutedByClass(), "com.automation.base.BaseFunction");
    }

    @Test
    public void shouldFailClearlyForUnknownApplicationAndUnknownKeyword() {
        FunctionResolver resolver = new FunctionResolver(testDriver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> execute(resolver, "UNKNOWN", "unknownKeyword", LOGIN_BUTTON_XPATH, "value")
        );

        Assert.assertTrue(exception.getMessage().contains(
                "Keyword 'unknownKeyword' not found in SpecificFunction for application 'UNKNOWN' or BaseFunction."
        ));
    }

    @Test
    public void shouldFailClearlyWhenKeywordDoesNotExistInSpecificOrBase() {
        FunctionResolver resolver = new FunctionResolver(testDriver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> execute(resolver, "HRIS", "selectRoomByName", ROOM_BUTTON_XPATH, "Meeting Room A")
        );

        Assert.assertTrue(exception.getMessage().contains(
                "Keyword 'selectRoomByName' not found in SpecificFunction for application 'HRIS' or BaseFunction."
        ));
    }

    @Test
    public void screenshotShouldNotResolveAsNormalFunctionKeyword() {
        FunctionResolver resolver = new FunctionResolver(testDriver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve("BRS", "screenshot")
        );

        Assert.assertEquals(
                exception.getMessage(),
                "Keyword 'screenshot' not found in SpecificFunction for application 'BRS' or BaseFunction."
        );
    }

    @Test
    public void shouldFailClearlyForBlankApplication() {
        FunctionResolver resolver = new FunctionResolver(testDriver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(" ", "click")
        );

        Assert.assertEquals(exception.getMessage(), "Application is required to resolve keyword 'click'.");
    }

    @Test
    public void shouldFailClearlyForBlankFunction() {
        FunctionResolver resolver = new FunctionResolver(testDriver().driver());

        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve("BRS", " ")
        );

        Assert.assertEquals(exception.getMessage(), "Function name is required.");
    }

    @Test
    public void shouldExecuteOtherApplicationSpecificFunctions() {
        FunctionResolver resolver = new FunctionResolver(testDriver().driver());

        FunctionExecutionResult hrisResult = execute(
                resolver,
                "HRIS",
                "verifyEmployeeVisible",
                PAGE_TITLE_XPATH,
                "Dashboard"
        );
        FunctionExecutionResult crmResult = execute(resolver, "CRM", "waitForApplicationReady", "", "");

        Assert.assertEquals(hrisResult.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertEquals(hrisResult.getExecutedByClass(), "com.automation.functions.HRIS.SpecificFunction");
        Assert.assertEquals(crmResult.getSourceType(), FunctionSourceType.SPECIFIC);
        Assert.assertEquals(crmResult.getExecutedByClass(), "com.automation.functions.CRM.SpecificFunction");
    }

    private void assertSpecificClass(ResolvedFunction resolvedFunction, String application) {
        Assert.assertEquals(resolvedFunction.getApplication(), application);
        Assert.assertEquals(resolvedFunction.getResolvedClassName(), "com.automation.functions." + application + ".SpecificFunction");
        Assert.assertEquals(resolvedFunction.getSourceType(), FunctionSourceType.SPECIFIC);
    }

    private FunctionExecutionResult execute(
            FunctionResolver resolver,
            String application,
            String function,
            String resolvedXpath,
            String resolvedValue
    ) {
        StepContextHolder.set(step(application, function, resolvedXpath, resolvedValue));
        try {
            return resolver.execute(application, function);
        } finally {
            StepContextHolder.clear();
        }
    }

    private ResolvedStepContext step(
            String application,
            String function,
            String resolvedXpath,
            String resolvedValue
    ) {
        return new ResolvedStepContext(
                "1",
                "Function Resolver Test",
                "Function Resolver Test",
                "Function Resolver Test",
                "Resolver keywords",
                2,
                3,
                1,
                function,
                resolvedXpath == null || resolvedXpath.isBlank() ? "" : "testObject",
                application,
                "Resolver keyword test",
                resolvedValue,
                resolvedValue,
                resolvedXpath,
                resolvedXpath,
                ""
        );
    }

    private FakeDriver testDriver() {
        FakeDriver fakeDriver = new FakeDriver();
        fakeDriver.title = "Dashboard - Function Resolver Test";
        fakeDriver.currentUrl = "file:///function-resolver-test.html";
        fakeDriver.addElement(USERNAME_XPATH, "");
        fakeDriver.addElement("//input[@id='password']", "");
        fakeDriver.addElement(LOGIN_BUTTON_XPATH, "Login");
        fakeDriver.addElement(ROOM_BUTTON_XPATH, "Meeting Room A");
        fakeDriver.addElement(MESSAGE_XPATH, "Booking created successfully");
        fakeDriver.addElement(PAGE_TITLE_XPATH, "Dashboard");
        return fakeDriver;
    }

    private static class FakeDriver implements InvocationHandler {

        private final Map<String, FakeElement> elements = new HashMap<>();
        private final List<String> scripts = new ArrayList<>();
        private final WebDriver proxy;
        private String currentUrl = "";
        private String title = "";

        private FakeDriver() {
            this.proxy = (WebDriver) java.lang.reflect.Proxy.newProxyInstance(
                    FunctionResolverTest.class.getClassLoader(),
                    new Class[]{WebDriver.class, JavascriptExecutor.class},
                    this
            );
        }

        private WebDriver driver() {
            return proxy;
        }

        private void addElement(String xpath, String text) {
            elements.put(key(By.xpath(xpath)), new FakeElement(text));
        }

        private FakeElement element(String xpath) {
            return elements.get(key(By.xpath(xpath)));
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "get" -> {
                    currentUrl = (String) args[0];
                    yield null;
                }
                case "getCurrentUrl" -> currentUrl;
                case "getTitle" -> title;
                case "findElement" -> findElement((By) args[0]);
                case "findElements" -> findElements((By) args[0]);
                case "executeScript" -> executeScript((String) args[0], args);
                case "executeAsyncScript" -> {
                    scripts.add((String) args[0]);
                    yield null;
                }
                case "getPageSource" -> "";
                case "close", "quit" -> null;
                case "getWindowHandles" -> Set.of("fake-window");
                case "getWindowHandle" -> "fake-window";
                case "toString" -> "FakeWebDriver";
                default -> throw new UnsupportedOperationException("Fake driver does not support " + method.getName() + ".");
            };
        }

        private WebElement findElement(By by) {
            FakeElement element = elements.get(key(by));
            if (element == null) {
                throw new NoSuchElementException("Missing element: " + by);
            }
            return element.webElement();
        }

        private List<WebElement> findElements(By by) {
            FakeElement element = elements.get(key(by));
            return element == null ? List.of() : List.of(element.webElement());
        }

        private Object executeScript(String script, Object[] args) {
            scripts.add(script);
            Object scriptArgument = scriptArgument(args);
            if (script.contains("click")
                    && scriptArgument != null
                    && java.lang.reflect.Proxy.isProxyClass(scriptArgument.getClass())) {
                InvocationHandler handler = java.lang.reflect.Proxy.getInvocationHandler(scriptArgument);
                if (handler instanceof FakeElement fakeElement) {
                    fakeElement.clicked = true;
                    fakeElement.javascriptClicked = true;
                }
            }
            return null;
        }

        private Object scriptArgument(Object[] args) {
            if (args.length <= 1) {
                return null;
            }
            Object scriptArgs = args[1];
            if (scriptArgs instanceof Object[] nestedArgs && nestedArgs.length > 0) {
                return nestedArgs[0];
            }
            return scriptArgs;
        }

        private String key(By by) {
            return by.toString();
        }
    }

    private static class FakeElement implements InvocationHandler {

        private final WebElement proxy;
        private final String text;
        private String value = "";
        private boolean clicked;
        private boolean javascriptClicked;
        private boolean displayed = true;
        private boolean enabled = true;

        private FakeElement(String text) {
            this.text = text;
            this.proxy = (WebElement) java.lang.reflect.Proxy.newProxyInstance(
                    FunctionResolverTest.class.getClassLoader(),
                    new Class[]{WebElement.class},
                    this
            );
        }

        private WebElement webElement() {
            return proxy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "click" -> {
                    clicked = true;
                    yield null;
                }
                case "submit" -> null;
                case "sendKeys" -> {
                    appendKeys(args[0]);
                    yield null;
                }
                case "clear" -> {
                    value = "";
                    yield null;
                }
                case "getTagName" -> "input";
                case "getAttribute", "getDomAttribute", "getDomProperty" -> "value".equals(args[0]) ? value : null;
                case "getAriaRole", "getAccessibleName", "getCssValue" -> "";
                case "isSelected" -> false;
                case "isEnabled" -> enabled;
                case "getText" -> text;
                case "findElements" -> List.of();
                case "findElement" -> throw new NoSuchElementException("Fake nested element missing: " + args[0]);
                case "getShadowRoot" -> throw new UnsupportedOperationException("Fake element does not support shadow root.");
                case "isDisplayed" -> displayed;
                case "getLocation" -> new Point(0, 0);
                case "getSize" -> new Dimension(100, 20);
                case "getRect" -> new Rectangle(new Point(0, 0), new Dimension(100, 20));
                case "getScreenshotAs" -> getScreenshot((OutputType<?>) args[0]);
                case "toString" -> "FakeWebElement";
                default -> throw new UnsupportedOperationException("Fake element does not support " + method.getName() + ".");
            };
        }

        private void appendKeys(Object keysArgument) {
            StringBuilder builder = new StringBuilder();
            if (keysArgument instanceof CharSequence[] keysToSend) {
                for (CharSequence keys : keysToSend) {
                    builder.append(keys);
                }
            } else if (keysArgument instanceof CharSequence key) {
                builder.append(key);
            }
            value += builder;
        }

        private <X> X getScreenshot(OutputType<X> target) {
            return null;
        }
    }
}
