package com.automation.tests;

import com.automation.config.ExcelExecutionConfig;
import com.automation.context.EvidenceContextHolder;
import com.automation.context.StepContextHolder;
import com.automation.engine.KeywordResolver;
import com.automation.engine.KeywordEngine;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ExecutionResult;
import com.automation.models.KeywordExecutionResult;
import com.automation.models.KeywordSourceType;
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
import org.openqa.selenium.WebElement;
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
        EvidenceContextHolder.clear();
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
        Assert.assertEquals(resolver.observedXPath, step.getResolvedXPath());
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
        Assert.assertEquals(result.getKeywordName(), "input");
        Assert.assertEquals(result.getObjectName(), "txtUsername");
        Assert.assertEquals(result.getApplication(), "BRS");
        Assert.assertEquals(result.getRawValue(), "LOGIN_DATA.USERNAME");
        Assert.assertEquals(result.getResolvedValue(), "brs_admin");
        Assert.assertEquals(result.getRawXPath(), "//input[@id='rawUsername']");
        Assert.assertEquals(result.getResolvedXPath(), "//input[@id='resolvedUsername']");
        Assert.assertEquals(result.getExecutedByClass(), "context.Executor");
        Assert.assertEquals(result.getExecutionSource(), KeywordSourceType.BASE.name());
        Assert.assertEquals(result.getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.getEvidence(), "");
        Assert.assertEquals(result.getExcelRowNumber(), 17);
        Assert.assertEquals(result.getStepOrder(), 3);
    }

    @Test
    public void executeShouldAttachEvidenceRegisteredByKeyword() {
        List<String> evidencePaths = List.of(
                "target/screenshots/custom-keyword-1.png",
                "target/screenshots/custom-keyword-2.png"
        );
        ObservingResolver resolver = new ObservingResolver(new FakeWebDriver(), false, evidencePaths);

        ExecutionResult result = engine(resolver).execute(step("customEvidence", "btnLogin", "", "", ""));

        Assert.assertTrue(result.isSuccess(), result.getMessage());
        Assert.assertEquals(result.getEvidence(), String.join(System.lineSeparator(), evidencePaths));
        Assert.assertTrue(EvidenceContextHolder.getAll().isEmpty());
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    @Test
    public void executeShouldAttachEvidenceRegisteredBeforeFailureAndClearEvidenceContext() {
        List<String> evidencePaths = List.of("target/screenshots/before-failure.png");
        ObservingResolver resolver = new ObservingResolver(new FakeWebDriver(), true, evidencePaths);

        ExecutionResult result = engine(resolver).execute(step("customEvidence", "btnLogin", "", "", ""));

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(result.getEvidence(), "target/screenshots/before-failure.png");
        Assert.assertTrue(result.getMessage().contains("Keyword 'customEvidence' failed: Synthetic keyword failure."));
        Assert.assertFalse(result.getMessage().contains("Cause:"));
        Assert.assertTrue(EvidenceContextHolder.getAll().isEmpty());
        Assert.assertTrue(StepContextHolder.current().isEmpty());
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
        Assert.assertTrue(messages.contains("START |"));
        Assert.assertTrue(messages.contains("Keyword = input"));
        Assert.assertTrue(messages.contains("Object = txtPassword"));
        Assert.assertTrue(messages.contains("XPath = //input[@id='resolvedUsername']"));
        Assert.assertTrue(messages.contains("Value = ****"));
        Assert.assertTrue(messages.contains("PASS |"));
        Assert.assertTrue(messages.contains("Source = BASE"));
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
        Assert.assertTrue(result.getMessage().contains("Sheet: Login Flow."));
        Assert.assertTrue(result.getMessage().contains("Testcase: Valid Login."));
        Assert.assertTrue(result.getMessage().contains("Row: 17."));
        Assert.assertTrue(result.getMessage().contains("Keyword: click."));
        Assert.assertTrue(result.getMessage().contains("Object: btnLogin."));
        Assert.assertTrue(result.getMessage().contains("Application: BRS."));
        Assert.assertTrue(result.getMessage().contains("Keyword 'click' failed: Synthetic keyword failure."));
        Assert.assertFalse(result.getMessage().contains("Cause:"));
        Assert.assertEquals(occurrences(result.getMessage(), "Scenario NO:"), 1);
    }

    @Test
    public void failureLoggingShouldEmitOneConciseEngineFailureEvent() {
        MessageAppender appender = new MessageAppender();
        Logger logger = (Logger) LogManager.getLogger(KeywordEngine.class);
        appender.start();
        logger.addAppender(appender);

        try {
            engine(new ObservingResolver(new FakeWebDriver(), true))
                    .execute(step("click", "btnLogin", "", "", ""));
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }

        long failureEvents = appender.messages().stream()
                .filter(message -> message.startsWith("FAIL |"))
                .count();
        Assert.assertEquals(failureEvents, 1L);
        Assert.assertTrue(appender.messages().stream().noneMatch(message -> message.contains("Cause: Cause:")));
    }

    @Test
    public void screenshotShouldUseResolvedContextAndProduceEvidence() {
        FakeWebDriver driver = new FakeWebDriver();
        KeywordResolver resolver = new KeywordResolver(driver.driver());
        RecordingScreenshotService screenshotService = new RecordingScreenshotService();
        ResolvedStepContext step = step("screenshot", "", "Manual checkpoint", "Login complete", "");

        ExecutionResult result = engine(resolver, screenshotService).execute(step);

        Assert.assertTrue(result.isSuccess(), result.getMessage());
        Assert.assertSame(screenshotService.observedContext, step);
        Assert.assertTrue(screenshotService.observedScreenshotName.contains("Execute an already-resolved step"));
        Assert.assertFalse(screenshotService.observedScreenshotName.contains("Login complete"));
        Assert.assertEquals(result.getExecutionSource(), "BASE");
        Assert.assertEquals(result.getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertEquals(result.getEvidence(), "target/screenshots/resolved-context.png");
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    @Test
    public void screenshotPartByObjectShouldUseResolvedObjectAndProduceMultipleEvidencePaths() {
        FakeWebDriver driver = new FakeWebDriver();
        driver.addElement("//input[@id='resolvedUsername']", "Long object");
        KeywordResolver resolver = new KeywordResolver(driver.driver());
        RecordingScreenshotService screenshotService = new RecordingScreenshotService();
        ResolvedStepContext step = step(
                "screenshotPartByObject",
                "pnlLongContent",
                "Long content evidence",
                "Booking form",
                ""
        );

        ExecutionResult result = engine(resolver, screenshotService).execute(step);

        Assert.assertTrue(result.isSuccess(), result.getMessage());
        Assert.assertSame(screenshotService.observedContext, step);
        Assert.assertTrue(screenshotService.observedScreenshotName.contains("Execute an already-resolved step"));
        Assert.assertFalse(screenshotService.observedScreenshotName.contains("Booking form"));
        Assert.assertEquals(result.getExecutionSource(), "BASE");
        Assert.assertEquals(result.getStatus(), ExecutionResult.STATUS_PASS);
        Assert.assertTrue(result.getEvidence().contains("target/screenshots/object-part-1.png"));
        Assert.assertTrue(result.getEvidence().contains("target/screenshots/object-part-2.png"));
        Assert.assertTrue(result.getEvidence().contains(System.lineSeparator()));
        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    private KeywordEngine engine(KeywordResolver resolver) {
        return engine(resolver, null);
    }

    private KeywordEngine engine(KeywordResolver resolver, ScreenshotService screenshotService) {
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
            String keyword,
            String objectName,
            String rawValue,
            String resolvedValue,
            String executedBy
    ) {
        return ResolvedStepContext.builder()
                .scenarioNo("SC-13C")
                .scenarioAction("Login Flow")
                .scenarioName("Resolved context execution")
                .sheetName("Login Flow")
                .testcaseName("Valid Login")
                .testcaseParentRow(14)
                .excelRow(17)
                .stepNumber(3)
                .keyword(keyword)
                .objectName(objectName)
                .application("BRS")
                .description("Execute an already-resolved step")
                .rawValue(rawValue)
                .resolvedValue(resolvedValue)
                .rawXPath("//input[@id='rawUsername']")
                .resolvedXPath("//input[@id='resolvedUsername']")
                .executedBy(executedBy)
                .build();
    }

    private static class ObservingResolver extends KeywordResolver {

        private final boolean fail;
        private final List<String> evidencePaths;
        private ResolvedStepContext observedContext;
        private String observedXPath;
        private String observedValue;

        private ObservingResolver(FakeWebDriver driver, boolean fail) {
            this(driver, fail, List.of());
        }

        private ObservingResolver(FakeWebDriver driver, boolean fail, List<String> evidencePaths) {
            super(driver.driver());
            this.fail = fail;
            this.evidencePaths = evidencePaths == null ? List.of() : List.copyOf(evidencePaths);
        }

        @Override
        public KeywordExecutionResult execute(String application, String keywordName) {
            observedContext = StepContextHolder.get();
            observedXPath = observedContext.getResolvedXPath();
            observedValue = observedContext.getResolvedValue();
            evidencePaths.forEach(EvidenceContextHolder::add);
            if (fail) {
                throw new FrameworkException("Synthetic keyword failure.");
            }
            return new KeywordExecutionResult(
                    application,
                    keywordName,
                    "resolver.Executor",
                    KeywordSourceType.BASE,
                    true,
                    "Synthetic keyword success."
            );
        }
    }

    private static class RecordingScreenshotService extends ScreenshotService {

        private ResolvedStepContext observedContext;
        private String observedScreenshotName;

        private RecordingScreenshotService() {
            super(Path.of("target", "screenshots"));
        }

        @Override
        public String capture(WebDriver driver, String screenshotName) {
            observedContext = StepContextHolder.get();
            observedScreenshotName = screenshotName;
            return "target/screenshots/resolved-context.png";
        }

        @Override
        public List<String> captureObjectInParts(
                WebDriver driver,
                WebElement element,
                ResolvedStepContext step,
                String screenshotName
        ) {
            observedContext = StepContextHolder.get();
            observedScreenshotName = screenshotName;
            return List.of(
                    "target/screenshots/object-part-1.png",
                    "target/screenshots/object-part-2.png"
            );
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

    private int occurrences(String value, String token) {
        return value.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}
