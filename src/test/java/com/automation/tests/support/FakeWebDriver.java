package com.automation.tests.support;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FakeWebDriver implements InvocationHandler {

    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77,
            0x53, (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49,
            0x44, 0x41, 0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8,
            (byte) 0xFF, (byte) 0xFF, 0x3F, 0x00, 0x05, (byte) 0xFE,
            0x02, (byte) 0xFE, (byte) 0xDC, (byte) 0xCC, 0x59,
            (byte) 0xE7, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45,
            0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    private final Map<String, FakeWebElement> elements = new HashMap<>();
    private final List<String> scripts = new ArrayList<>();
    private final WebDriver proxy;
    private String currentUrl = "";
    private String title = "";

    public FakeWebDriver() {
            this.proxy = (WebDriver) Proxy.newProxyInstance(
                FakeWebDriver.class.getClassLoader(),
                new Class[]{WebDriver.class, JavascriptExecutor.class, TakesScreenshot.class},
                this
        );
    }

    public WebDriver driver() {
        return proxy;
    }

    public FakeWebElement addElement(String xpath, String text) {
        FakeWebElement element = new FakeWebElement(text);
        elements.put(key(By.xpath(xpath)), element);
        return element;
    }

    public FakeWebElement element(String xpath) {
        return elements.get(key(By.xpath(xpath)));
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public void setCurrentUrl(String currentUrl) {
        this.currentUrl = currentUrl == null ? "" : currentUrl;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public List<String> getScripts() {
        return scripts;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        Object[] safeArgs = args == null ? new Object[0] : args;
        return switch (method.getName()) {
            case "get" -> {
                currentUrl = (String) safeArgs[0];
                yield null;
            }
            case "getCurrentUrl" -> currentUrl;
            case "getTitle" -> title;
            case "findElement" -> findElement((By) safeArgs[0]);
            case "findElements" -> findElements((By) safeArgs[0]);
            case "executeScript" -> executeScript((String) safeArgs[0], safeArgs);
            case "executeAsyncScript" -> {
                scripts.add((String) safeArgs[0]);
                yield null;
            }
            case "getScreenshotAs" -> getScreenshot((OutputType<?>) safeArgs[0]);
            case "getPageSource" -> "";
            case "close", "quit" -> null;
            case "getWindowHandles" -> Set.of("fake-window");
            case "getWindowHandle" -> "fake-window";
            case "toString" -> "FakeWebDriver";
            default -> throw new UnsupportedOperationException("Fake driver does not support " + method.getName() + ".");
        };
    }

    private WebElement findElement(By by) {
        FakeWebElement element = elements.get(key(by));
        if (element == null) {
            throw new NoSuchElementException("Missing element: " + by);
        }
        return element.webElement();
    }

    private List<WebElement> findElements(By by) {
        FakeWebElement element = elements.get(key(by));
        return element == null ? List.of() : List.of(element.webElement());
    }

    private Object executeScript(String script, Object[] args) {
        scripts.add(script);
        if ("return document.readyState".equals(script)) {
            return "complete";
        }

        Object scriptArgument = scriptArgument(args);
        if (script.contains("click")
                && scriptArgument != null
                && Proxy.isProxyClass(scriptArgument.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(scriptArgument);
            if (handler instanceof FakeWebElement element) {
                element.clicked = true;
                element.javascriptClicked = true;
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

    private <X> X getScreenshot(OutputType<X> target) {
        return target.convertFromPngBytes(PNG_BYTES);
    }

    public static class FakeWebElement implements InvocationHandler {

        private final WebElement proxy;
        private final String text;
        private String value = "";
        private boolean clicked;
        private boolean javascriptClicked;
        private boolean displayed = true;
        private boolean enabled = true;

        private FakeWebElement(String text) {
            this.text = text;
            this.proxy = (WebElement) Proxy.newProxyInstance(
                    FakeWebDriver.class.getClassLoader(),
                    new Class[]{WebElement.class},
                    this
            );
        }

        public WebElement webElement() {
            return proxy;
        }

        public String getValue() {
            return value;
        }

        public boolean isClicked() {
            return clicked;
        }

        public boolean isJavascriptClicked() {
            return javascriptClicked;
        }

        public void setDisplayed(boolean displayed) {
            this.displayed = displayed;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object[] safeArgs = args == null ? new Object[0] : args;
            return switch (method.getName()) {
                case "click" -> {
                    clicked = true;
                    yield null;
                }
                case "submit" -> null;
                case "sendKeys" -> {
                    appendKeys(safeArgs[0]);
                    yield null;
                }
                case "clear" -> {
                    value = "";
                    yield null;
                }
                case "getTagName" -> "input";
                case "getAttribute", "getDomAttribute", "getDomProperty" -> "value".equals(safeArgs[0]) ? value : null;
                case "getAriaRole", "getAccessibleName", "getCssValue" -> "";
                case "isSelected" -> false;
                case "isEnabled" -> enabled;
                case "getText" -> text;
                case "findElements" -> List.of();
                case "findElement" -> throw new NoSuchElementException("Fake nested element missing: " + safeArgs[0]);
                case "getShadowRoot" -> throw new UnsupportedOperationException("Fake element does not support shadow root.");
                case "isDisplayed" -> displayed;
                case "getLocation" -> new Point(0, 0);
                case "getSize" -> new Dimension(100, 20);
                case "getRect" -> new Rectangle(new Point(0, 0), new Dimension(100, 20));
                case "getScreenshotAs" -> getScreenshot((OutputType<?>) safeArgs[0]);
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
