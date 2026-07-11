package com.automation.tests;

import com.automation.models.ResolvedStepContext;
import com.automation.services.ScreenshotService;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScreenshotServiceScrollingTest {

    @Test
    public void objectCaptureShouldScreenshotEachScrollableElementPartAndRestoreTop() {
        ScrollingDriver scrollingDriver = new ScrollingDriver(ScrollMode.OBJECT);
        RecordingScreenshotService service = new RecordingScreenshotService();

        List<String> evidence = service.captureObjectInParts(
                scrollingDriver.driver(),
                scrollingDriver.element(),
                step("screenshotPartByObject", "pnlBooking", "Booking panel"),
                "Booking panel"
        );

        Assert.assertEquals(evidence.size(), 3);
        Assert.assertEquals(
                service.labels,
                List.of("Booking panel_part1", "Booking panel_part2", "Booking panel_part3")
        );
        Assert.assertEquals(scrollingDriver.elementScrollTop, 0.0);
        Assert.assertTrue(scrollingDriver.scripts.stream().anyMatch(script -> script.contains("arguments[0].scrollTop")));
    }

    @Test
    public void fullPageCaptureShouldUseDocumentScrollerForEachPartAndRestoreTop() {
        ScrollingDriver scrollingDriver = new ScrollingDriver(ScrollMode.FULL_PAGE);
        RecordingScreenshotService service = new RecordingScreenshotService();

        List<String> evidence = service.captureFullPageInParts(
                scrollingDriver.driver(),
                step("screenshotFullPart", "", "Entire page"),
                "Entire page"
        );

        Assert.assertEquals(evidence.size(), 3);
        Assert.assertEquals(
                service.labels,
                List.of("Entire page_part1", "Entire page_part2", "Entire page_part3")
        );
        Assert.assertEquals(scrollingDriver.pageScrollTop, 0.0);
        Assert.assertTrue(scrollingDriver.scripts.stream().anyMatch(
                script -> script.contains("document.scrollingElement")
        ));
    }

    private ResolvedStepContext step(String keyword, String objectName, String description) {
        return ResolvedStepContext.builder()
                .scenarioNo("26E")
                .scenarioAction("Screenshot Polish")
                .scenarioName("Screenshot polish")
                .sheetName("Screenshot Polish")
                .testcaseName("Scrolling screenshots")
                .testcaseParentRow(2)
                .excelRow(3)
                .stepNumber(1)
                .keyword(keyword)
                .objectName(objectName)
                .application("BRS")
                .description(description)
                .rawValue("")
                .resolvedValue("")
                .rawXPath("//section")
                .resolvedXPath("//section")
                .executedBy("")
                .build();
    }

    private enum ScrollMode {
        OBJECT,
        FULL_PAGE
    }

    private static final class ScrollingDriver implements InvocationHandler {

        private final ScrollMode mode;
        private final List<String> scripts = new ArrayList<>();
        private final WebDriver driver;
        private final WebElement element;
        private double elementScrollTop;
        private double pageScrollTop;

        private ScrollingDriver(ScrollMode mode) {
            this.mode = mode;
            this.driver = (WebDriver) Proxy.newProxyInstance(
                    ScreenshotServiceScrollingTest.class.getClassLoader(),
                    new Class[]{WebDriver.class, JavascriptExecutor.class},
                    this
            );
            this.element = (WebElement) Proxy.newProxyInstance(
                    ScreenshotServiceScrollingTest.class.getClassLoader(),
                    new Class[]{WebElement.class},
                    (proxy, method, args) -> "toString".equals(method.getName()) ? "ScrollableElement" : null
            );
        }

        private WebDriver driver() {
            return driver;
        }

        private WebElement element() {
            return element;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("executeScript".equals(method.getName())) {
                return executeScript((String) args[0]);
            }
            if ("toString".equals(method.getName())) {
                return "ScrollingDriver";
            }
            throw new UnsupportedOperationException("Unexpected driver method: " + method.getName());
        }

        private Object executeScript(String script) {
            scripts.add(script);
            return mode == ScrollMode.OBJECT ? executeObjectScript(script) : executeFullPageScript(script);
        }

        private Object executeObjectScript(String script) {
            if (script.contains("return arguments[0].scrollHeight")) {
                return 250.0;
            }
            if (script.contains("return arguments[0].clientHeight")) {
                return 100.0;
            }
            if (script.contains("arguments[0].scrollTop = Math.min")) {
                elementScrollTop = Math.min(elementScrollTop + 100.0, 150.0);
                return null;
            }
            if (script.contains("arguments[0].scrollTop = 0")) {
                elementScrollTop = 0.0;
                return null;
            }
            if (script.contains("return arguments[0].scrollTop")) {
                return elementScrollTop;
            }
            return null;
        }

        private Object executeFullPageScript(String script) {
            if (script.contains("return Math.max")) {
                return 150.0;
            }
            if (script.contains("scroller.scrollTop = Math.min")) {
                pageScrollTop = Math.min(pageScrollTop + 100.0, 150.0);
                return null;
            }
            if (script.contains("return scroller.scrollTop")) {
                return pageScrollTop;
            }
            if (script.contains("scroller.scrollTop = 0")) {
                pageScrollTop = 0.0;
            }
            return null;
        }
    }

    private static final class RecordingScreenshotService extends ScreenshotService {

        private final List<String> labels = new ArrayList<>();

        private RecordingScreenshotService() {
            super(Path.of("target", "phase-26e-scrolling"));
        }

        @Override
        public String captureScreen(WebDriver driver, ResolvedStepContext step, String label) {
            labels.add(label);
            return Path.of("target", "phase-26e-scrolling", label + ".png").toString();
        }

        @Override
        protected void pauseAfterScroll() {
            // Scrolling is deterministic in this focused unit test.
        }
    }
}
