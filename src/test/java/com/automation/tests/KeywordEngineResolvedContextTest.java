package com.automation.tests;

import com.automation.config.ExcelExecutionConfig;
import com.automation.context.StepContextHolder;
import com.automation.engine.FunctionResolver;
import com.automation.engine.KeywordEngine;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ExecutionResult;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.FunctionSourceType;
import com.automation.models.ResolvedStepContext;
import com.automation.reports.ExcelReportConfig;
import com.automation.services.ScreenshotService;
import com.automation.tests.support.FakeWebDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Test(singleThreaded = true)
public class KeywordEngineResolvedContextTest {

    private static final Path TEMPLATE_FILE = Path.of(
            "src", "test", "resources", "testdata", "Template Testing.xlsx"
    );

    private ExcelReader excelReader;

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        StepContextHolder.clear();
        if (excelReader != null) {
            excelReader.close();
            excelReader = null;
        }
    }

    @Test
    public void executeShouldSetContextBeforeKeywordExecution() {
        ResolvedStepContext step = step("click", "btnLogin", "", "", "context.Executor");
        ObservingResolver resolver = new ObservingResolver(new FakeWebDriver(), false);

        ExecutionResult result = engine(resolver).execute(step);

        Assert.assertTrue(result.isSuccess(), result.getMessage());
        Assert.assertSame(resolver.observedContext, step);
        Assert.assertEquals(resolver.observedXpath, step.getResolvedXPath());
        Assert.assertEquals(resolver.observedValue, step.getResolvedValue());
    }

    @Test
    public void executeShouldClearContextAfterSuccessfulExecution() {
        ExecutionResult result = engine(new ObservingResolver(new FakeWebDriver(), false))
                .execute(step("click", "btnLogin", "", "", ""));

        Assert.assertTrue(result.isSuccess(), result.getMessage());
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    @Test
    public void executeShouldClearContextAfterFailedExecution() {
        ExecutionResult result = engine(new ObservingResolver(new FakeWebDriver(), true))
                .execute(step("click", "btnLogin", "", "", ""));

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    @Test
    public void executionResultShouldUseResolvedContextFields() {
        ResolvedStepContext step = step(
                "input",
                "txtUsername",
                "LOGIN_DATA.USERNAME",
                "brs_admin",
                "context.Executor"
        );

        ExecutionResult result = engine(new ObservingResolver(new FakeWebDriver(), false)).execute(step);

        Assert.assertEquals(result.getScenarioNo(), "SC-13C");
        Assert.assertEquals(result.getScenarioName(), "Resolved context execution");
        Assert.assertEquals(result.getScenarioAction(), "Login Flow");
        Assert.assertEquals(result.getTestcaseName(), "Valid Login");
        Assert.assertEquals(result.getDescription(), "Execute an already-resolved step");
        Assert.assertEquals(result.getFunctionName(), "input");
        Assert.assertEquals(result.getObjectName(), "txtUsername");
        Assert.assertEquals(result.getApplication(), "BRS");
        Assert.assertEquals(result.getRawValue(), "LOGIN_DATA.USERNAME");
        Assert.assertEquals(result.getResolvedValue(), "brs_admin");
        Assert.assertEquals(result.getRawXpath(), "//input[@id='rawUsername']");
        Assert.assertEquals(result.getResolvedXpath(), "//input[@id='resolvedUsername']");
        Assert.assertEquals(result.getExecutedByClass(), "context.Executor");
        Assert.assertEquals(result.getExecutionSource(), FunctionSourceType.BASE.name());
        Assert.assertEquals(result.getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.getEvidence(), "");
        Assert.assertEquals(result.getExcelRowNumber(), 17);
        Assert.assertEquals(result.getStepOrder(), 3);
    }

    @Test
    public void lifecycleLoggingShouldBeCentralizedAndMaskSensitiveValue() {
        String secret = "super-secret-password";
        ResolvedStepContext step = step(
                "input",
                "txtPassword",
                "LOGIN_DATA.PASSWORD",
                secret,
                ""
        );
        MessageAppender appender = new MessageAppender();
        Logger logger = (Logger) LogManager.getLogger(KeywordEngine.class);
        appender.start();
        logger.addAppender(appender);

        try {
            ExecutionResult result = engine(new ObservingResolver(new FakeWebDriver(), false)).execute(step);
            Assert.assertTrue(result.isSuccess(), result.getMessage());
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }

        String messages = String.join(System.lineSeparator(), appender.messages());
        Assert.assertTrue(messages.contains("Executing keyword."));
        Assert.assertTrue(messages.contains("Function = input"));
        Assert.assertTrue(messages.contains("Object = txtPassword"));
        Assert.assertTrue(messages.contains("XPath = //input[@id='resolvedUsername']"));
        Assert.assertTrue(messages.contains("Value = ****"));
        Assert.assertTrue(messages.contains("Completed keyword."));
        Assert.assertFalse(messages.contains(secret));
    }

    @Test
    public void executeShouldRejectNullStepClearly() {
        IllegalArgumentException exception = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> engine(new ObservingResolver(new FakeWebDriver(), false))
                        .execute((ResolvedStepContext) null)
        );

        Assert.assertEquals(exception.getMessage(), "ResolvedStepContext must not be null.");
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    @Test
    public void failureShouldIncludeCompleteResolvedStepContext() {
        ResolvedStepContext step = step("click", "btnLogin", "", "", "");

        ExecutionResult result = engine(new ObservingResolver(new FakeWebDriver(), true)).execute(step);

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getMessage().contains("Scenario NO: SC-13C."));
        Assert.assertTrue(result.getMessage().contains("Scenario ACTION: Login Flow."));
        Assert.assertTrue(result.getMessage().contains("Testcase: Valid Login."));
        Assert.assertTrue(result.getMessage().contains("Row: 17."));
        Assert.assertTrue(result.getMessage().contains("Function: click."));
        Assert.assertTrue(result.getMessage().contains("Object: btnLogin."));
        Assert.assertTrue(result.getMessage().contains("Application: BRS."));
        Assert.assertTrue(result.getMessage().contains("Cause: Synthetic keyword failure."));
    }

    @Test
    public void screenshotShouldUseResolvedContextAndProduceEvidence() {
        FakeWebDriver driver = new FakeWebDriver();
        ObservingResolver resolver = new ObservingResolver(driver, true);
        RecordingScreenshotService screenshotService = new RecordingScreenshotService();
        ResolvedStepContext step = step("screenshot", "", "Manual checkpoint", "Login complete", "");

        ExecutionResult result = engine(resolver, screenshotService).execute(step);

        Assert.assertTrue(result.isSuccess(), result.getMessage());
        Assert.assertNull(resolver.observedContext, "Screenshot should remain KeywordEngine special handling.");
        Assert.assertSame(screenshotService.observedContext, step);
        Assert.assertEquals(result.getExecutionSource(), "REPORT");
        Assert.assertEquals(result.getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.getEvidence(), "target/screenshots/resolved-context.png");
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    private KeywordEngine engine(FunctionResolver resolver) {
        return engine(resolver, null);
    }

    private KeywordEngine engine(FunctionResolver resolver, ScreenshotService screenshotService) {
        excelReader = new ExcelReader(TEMPLATE_FILE.toString());
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        return new KeywordEngine(
                dataReader,
                objectRepositoryReader,
                resolver,
                new ExcelReportConfig(false, true, true),
                ExcelExecutionConfig.load(),
                screenshotService
        );
    }

    private ResolvedStepContext step(
            String function,
            String objectName,
            String rawValue,
            String resolvedValue,
            String executedBy
    ) {
        return new ResolvedStepContext(
                "SC-13C",
                "Login Flow",
                "Resolved context execution",
                "Login Flow",
                "Valid Login",
                14,
                17,
                3,
                function,
                objectName,
                "BRS",
                "Execute an already-resolved step",
                rawValue,
                resolvedValue,
                "//input[@id='rawUsername']",
                "//input[@id='resolvedUsername']",
                executedBy
        );
    }

    private static class ObservingResolver extends FunctionResolver {

        private final boolean fail;
        private ResolvedStepContext observedContext;
        private String observedXpath;
        private String observedValue;

        private ObservingResolver(FakeWebDriver driver, boolean fail) {
            super(driver.driver());
            this.fail = fail;
        }

        @Override
        public FunctionExecutionResult execute(String application, String functionName) {
            observedContext = StepContextHolder.get();
            observedXpath = observedContext.getResolvedXPath();
            observedValue = observedContext.getResolvedValue();
            if (fail) {
                throw new FrameworkException("Synthetic keyword failure.");
            }
            return new FunctionExecutionResult(
                    application,
                    functionName,
                    "resolver.Executor",
                    FunctionSourceType.BASE,
                    true,
                    "Synthetic keyword success."
            );
        }
    }

    private static class RecordingScreenshotService extends ScreenshotService {

        private ResolvedStepContext observedContext;

        private RecordingScreenshotService() {
            super(Path.of("target", "screenshots"));
        }

        @Override
        public String capture(WebDriver driver, String screenshotName) {
            observedContext = StepContextHolder.get();
            return "target/screenshots/resolved-context.png";
        }
    }

    private static class MessageAppender extends AbstractAppender {

        private final List<String> messages = Collections.synchronizedList(new ArrayList<>());

        private MessageAppender() {
            super(
                    "KeywordEngineResolvedContextTestAppender",
                    null,
                    PatternLayout.createDefaultLayout(),
                    false,
                    Property.EMPTY_ARRAY
            );
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }

        private List<String> messages() {
            return List.copyOf(messages);
        }
    }
}
