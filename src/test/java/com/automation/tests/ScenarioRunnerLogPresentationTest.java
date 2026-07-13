package com.automation.tests;

import com.automation.config.ExcelExecutionConfig;
import com.automation.engine.KeywordEngine;
import com.automation.engine.KeywordResolver;
import com.automation.engine.ScenarioRunner;
import com.automation.excel.DataReader;
import com.automation.excel.ExcelReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.ExecutionResult;
import com.automation.models.ResolvedStepContext;
import com.automation.reports.ExcelReportConfig;
import com.automation.tests.support.ExcelKeywordTestWorkbookFactory;
import com.automation.tests.support.FakeWebDriver;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Test(singleThreaded = true)
public class ScenarioRunnerLogPresentationTest {

    private static final Path TEMP_DIR = Path.of("target", "phase-26f-runner-logs");
    private Path workbook;

    @BeforeClass
    public void createWorkbook() throws IOException {
        workbook = ExcelKeywordTestWorkbookFactory.createWorkbook(
                TEMP_DIR.resolve("runner-logs.xlsx"),
                Path.of("src", "test", "resources", "test-pages", "excel-keyword-test.html")
                        .toAbsolutePath()
                        .toUri()
                        .toString()
        );
    }

    @Test
    public void successfulRunShouldUseShortScenarioAndTestcaseBoundaries() {
        try (ExcelReader excelReader = new ExcelReader(workbook.toString())) {
            RunnerAppender appender = attachRunnerAppender();
            try {
                List<ExecutionResult> results = runner(excelReader, false).runActiveScenarios();

                Assert.assertTrue(results.stream().allMatch(ExecutionResult::isSuccess));
                Assert.assertTrue(appender.messages().contains("Scenario started: [1] Local Keyword Test"));
                Assert.assertTrue(appender.messages().contains("Testcase started: Login BRS"));
                Assert.assertTrue(appender.messages().stream().anyMatch(
                        message -> message.equals(
                                "[1] | Row = 5 | input | txtUsername | brs_admin"
                        )
                ));
                Assert.assertTrue(appender.messages().stream().anyMatch(
                        message -> message.equals(
                                "[1] | Row = 6 | input | txtPassword | ****"
                        )
                ));
                Assert.assertTrue(appender.messages().contains("Testcase passed: Login BRS"));
                Assert.assertTrue(appender.messages().contains("Scenario passed: [1] Local Keyword Test"));
                Assert.assertTrue(appender.messages().stream().noneMatch(message -> message.contains("Resolved scenario")));
            } finally {
                detachRunnerAppender(appender);
            }
        }
    }

    @Test
    public void failedRunShouldLogOneStopBoundaryWithoutRepeatingFailureText() {
        try (ExcelReader excelReader = new ExcelReader(workbook.toString())) {
            RunnerAppender appender = attachRunnerAppender();
            try {
                List<ExecutionResult> results = runner(excelReader, true).runActiveScenarios();

                Assert.assertEquals(results.size(), 1);
                Assert.assertFalse(results.get(0).isSuccess());
                Assert.assertEquals(
                        appender.messages().stream().filter(message -> message.startsWith("Scenario stopped:")).count(),
                        1L
                );
                Assert.assertTrue(appender.messages().stream().noneMatch(message -> message.startsWith("[1] | Row =")));
                Assert.assertTrue(appender.messages().stream().noneMatch(
                        message -> message.contains("Synthetic concise failure")
                ));
                Assert.assertTrue(appender.events().stream().noneMatch(event -> event.level() == Level.ERROR));
            } finally {
                detachRunnerAppender(appender);
            }
        }
    }

    private ScenarioRunner runner(ExcelReader excelReader, boolean failImmediately) {
        FakeWebDriver driver = driver();
        DataReader dataReader = new DataReader(excelReader);
        ObjectRepositoryReader objectRepositoryReader = new ObjectRepositoryReader(excelReader, dataReader);
        ExcelExecutionConfig executionConfig = executionConfig();
        KeywordEngine engine = failImmediately
                ? new FailingKeywordEngine(dataReader, objectRepositoryReader, driver, executionConfig)
                : new KeywordEngine(
                        dataReader,
                        objectRepositoryReader,
                        new KeywordResolver(driver.driver()),
                        new ExcelReportConfig(false, false, true),
                        executionConfig
                );
        return new ScenarioRunner(new ScenarioReader(excelReader), new StepReader(excelReader), engine);
    }

    private ExcelExecutionConfig executionConfig() {
        Properties properties = new Properties();
        properties.setProperty(ExcelExecutionConfig.SCENARIO_FILE_PATH_KEY, workbook.toString());
        properties.setProperty(ExcelExecutionConfig.REPORT_OUTPUT_DIRECTORY_KEY, TEMP_DIR.resolve("reports").toString());
        return ExcelExecutionConfig.fromProperties(properties, Map.of());
    }

    private FakeWebDriver driver() {
        FakeWebDriver driver = new FakeWebDriver();
        driver.addElement("//input[@id='username']", "");
        driver.addElement("//input[@id='password']", "");
        driver.addElement("//button[@id='loginButton']", "Login");
        driver.addElement("//h1[@id='dashboard']", "Dashboard");
        driver.addElement("//input[@id='bookingTitle']", "");
        driver.addElement("//button[contains(text(),'Meeting Room A')]", "Meeting Room A");
        driver.addElement("//div[@id='message']", "Booking created successfully");
        return driver;
    }

    private RunnerAppender attachRunnerAppender() {
        RunnerAppender appender = new RunnerAppender();
        appender.start();
        ((Logger) LogManager.getLogger(ScenarioRunner.class)).addAppender(appender);
        return appender;
    }

    private void detachRunnerAppender(RunnerAppender appender) {
        ((Logger) LogManager.getLogger(ScenarioRunner.class)).removeAppender(appender);
        appender.stop();
    }

    private static final class FailingKeywordEngine extends KeywordEngine {

        private FailingKeywordEngine(
                DataReader dataReader,
                ObjectRepositoryReader objectRepositoryReader,
                FakeWebDriver driver,
                ExcelExecutionConfig executionConfig
        ) {
            super(
                    dataReader,
                    objectRepositoryReader,
                    new KeywordResolver(driver.driver()),
                    new ExcelReportConfig(false, false, true),
                    executionConfig
            );
        }

        @Override
        public ExecutionResult execute(ResolvedStepContext step) {
            return ExecutionResult.failure(
                    step,
                    FailingKeywordEngine.class.getName(),
                    "TEST",
                    "Synthetic concise failure."
            );
        }
    }

    private static final class RunnerAppender extends AbstractAppender {

        private final List<RunnerEvent> events = Collections.synchronizedList(new ArrayList<>());

        private RunnerAppender() {
            super(
                    "ScenarioRunnerLogPresentationAppender",
                    null,
                    PatternLayout.createDefaultLayout(),
                    false,
                    Property.EMPTY_ARRAY
            );
        }

        @Override
        public void append(LogEvent event) {
            events.add(new RunnerEvent(event.getLevel(), event.getMessage().getFormattedMessage()));
        }

        private List<RunnerEvent> events() {
            return List.copyOf(events);
        }

        private List<String> messages() {
            return events().stream().map(RunnerEvent::message).toList();
        }
    }

    private record RunnerEvent(Level level, String message) {
    }
}
