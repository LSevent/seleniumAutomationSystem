package com.automation.engine;

import com.automation.context.StepContextHolder;
import com.automation.excel.DataReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.exceptions.ErrorContext;
import com.automation.config.ExcelExecutionConfig;
import com.automation.models.ExecutionResult;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.ResolvedObject;
import com.automation.models.ResolvedStepContext;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.models.TestStep;
import com.automation.reports.ExcelReportConfig;
import com.automation.reports.SensitiveDataMasker;
import com.automation.services.ScreenshotService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;

public class KeywordEngine {

    private static final Logger LOGGER = LogManager.getLogger(KeywordEngine.class);
    private static final SensitiveDataMasker SENSITIVE_DATA_MASKER = new SensitiveDataMasker();
    private static final String MANUAL_SCREENSHOT_DISABLED_MESSAGE = "Manual screenshot skipped because report.manualScreenshotEnabled=false.";

    private final DataReader dataReader;
    private final ObjectRepositoryReader objectRepositoryReader;
    private final FunctionResolver functionResolver;
    private final WebDriver driver;
    private final ExcelReportConfig reportConfig;
    private final ExcelExecutionConfig executionConfig;
    private final ScreenshotService screenshotService;

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            FunctionResolver functionResolver
    ) {
        this(dataReader, objectRepositoryReader, functionResolver, ExcelReportConfig.fromConfig());
    }

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            FunctionResolver functionResolver,
            ExcelReportConfig reportConfig
    ) {
        this(dataReader, objectRepositoryReader, functionResolver, reportConfig, ExcelExecutionConfig.load());
    }

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            FunctionResolver functionResolver,
            ExcelReportConfig reportConfig,
            ExcelExecutionConfig executionConfig
    ) {
        this(dataReader, objectRepositoryReader, functionResolver, reportConfig, executionConfig, null);
    }

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            FunctionResolver functionResolver,
            ExcelReportConfig reportConfig,
            ExcelExecutionConfig executionConfig,
            ScreenshotService screenshotService
    ) {
        if (dataReader == null) {
            throw new IllegalArgumentException("DataReader must not be null.");
        }
        if (objectRepositoryReader == null) {
            throw new IllegalArgumentException("ObjectRepositoryReader must not be null.");
        }
        if (functionResolver == null) {
            throw new IllegalArgumentException("FunctionResolver must not be null.");
        }
        this.dataReader = dataReader;
        this.objectRepositoryReader = objectRepositoryReader;
        this.functionResolver = functionResolver;
        this.driver = functionResolver.getDriver();
        this.reportConfig = reportConfig == null ? ExcelReportConfig.fromConfig() : reportConfig;
        this.executionConfig = executionConfig == null ? ExcelExecutionConfig.load() : executionConfig;
        this.screenshotService = screenshotService == null
                ? new ScreenshotService(this.executionConfig.getScreenshotOutputDirectory())
                : screenshotService;
    }

    public ExecutionResult execute(ResolvedStepContext step) {
        if (step == null) {
            throw new IllegalArgumentException("ResolvedStepContext must not be null.");
        }

        StepContextHolder.set(step);
        try {
            logResolvedStepStarted(step);

            ExecutionResult result;
            if (isBlank(step.getKeyword())) {
                result = ExecutionResult.failure(
                        step,
                        KeywordEngine.class.getName(),
                        "ENGINE",
                        failureMessage(
                                "Keyword is required in sheet " + safe(step.getSheetName())
                                        + " row " + step.getExcelRow() + ".",
                                step,
                                null
                        )
                );
            } else if (isScreenshotKeyword(step.getKeyword())) {
                result = executeManualScreenshot(step);
            } else {
                result = executeResolvedKeyword(step);
            }
            return logResolvedResult(result);
        } finally {
            StepContextHolder.clear();
        }
    }

    public ExecutionResult executeStep(Scenario scenario, TestStep step) {
        validateScenarioAndStep(scenario, step);

        ExecutionContext context = new ExecutionContext();
        context.setScenario(scenario);
        context.setTestStep(step);
        context.setCurrentStepNumber(step.getStepOrder());

        LOGGER.info(
                "Step started. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, Object = {}",
                scenario.getNo(),
                scenario.getAction(),
                step.getTestcaseName(),
                step.getExcelRowNumber(),
                step.getKeyword(),
                step.getObject()
        );

        if (isBlank(step.getKeyword())) {
            return logFailure(failure(context, "Keyword is required in sheet " + scenario.getAction() + " row " + step.getExcelRowNumber() + "."));
        }

        if (!resolveValue(context)) {
            return logFailure(failureFromContext(context));
        }
        if (isScreenshotKeyword(step.getKeyword())) {
            return logManualScreenshot(executeManualScreenshot(context));
        }
        if (!resolveObject(context)) {
            return logFailure(failureFromContext(context));
        }

        ExecutionResult result = executeKeyword(context);
        if (ExecutionResult.STATUS_SKIP.equals(result.getStatus())) {
            LOGGER.info(
                    "Step skipped. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, Message = {}",
                    scenario.getNo(),
                    scenario.getAction(),
                    step.getTestcaseName(),
                    step.getExcelRowNumber(),
                    step.getKeyword(),
                    result.getMessage()
            );
        } else if (result.isSuccess()) {
            LOGGER.info(
                    "Step passed. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, Source = {}",
                    scenario.getNo(),
                    scenario.getAction(),
                    step.getTestcaseName(),
                    step.getExcelRowNumber(),
                    step.getKeyword(),
                    result.getExecutionSource()
            );
        } else {
            logFailure(result);
        }
        return result;
    }

    public List<ExecutionResult> executeSteps(Scenario scenario, List<TestStep> steps) {
        if (steps == null) {
            throw new IllegalArgumentException("Steps must not be null.");
        }

        List<ExecutionResult> results = new ArrayList<>();
        for (TestStep step : steps) {
            ExecutionResult result = executeStep(scenario, step);
            results.add(result);
            if (!result.isSuccess()) {
                break;
            }
        }
        return results;
    }

    DataReader getDataReader() {
        return dataReader;
    }

    ObjectRepositoryReader getObjectRepositoryReader() {
        return objectRepositoryReader;
    }

    private ExecutionResult executeResolvedKeyword(ResolvedStepContext step) {
        try {
            FunctionExecutionResult keywordResult = functionResolver.execute(
                    step.getApplication(),
                    step.getKeyword()
            );
            String executedBy = isBlank(step.getExecutedBy())
                    ? keywordResult.getExecutedByClass()
                    : step.getExecutedBy();
            return ExecutionResult.success(
                    step,
                    executedBy,
                    keywordResult.getSourceType().name(),
                    keywordResult.getMessage()
            );
        } catch (RuntimeException | AssertionError exception) {
            String message = "Keyword '" + safe(step.getKeyword()) + "' failed at step row "
                    + step.getExcelRow() + ".";
            if (!containsStepContext(exception.getMessage())) {
                message += System.lineSeparator() + resolvedStepContext(step);
            }
            message += System.lineSeparator() + "Cause: " + safe(exception.getMessage());
            return ExecutionResult.failure(step, safe(step.getExecutedBy()), "KEYWORD", message);
        }
    }

    private ExecutionResult executeManualScreenshot(ResolvedStepContext step) {
        String executedBy = isBlank(step.getExecutedBy())
                ? KeywordEngine.class.getName()
                : step.getExecutedBy();
        if (!reportConfig.isManualScreenshotEnabled()) {
            return ExecutionResult.skipped(
                    step,
                    executedBy,
                    "REPORT",
                    MANUAL_SCREENSHOT_DISABLED_MESSAGE,
                    MANUAL_SCREENSHOT_DISABLED_MESSAGE
            );
        }

        try {
            String label = isBlank(step.getResolvedValue()) ? "ManualScreenshot" : step.getResolvedValue();
            String screenshotPath = screenshotService.capture(driver, screenshotBaseName(step, label));
            String evidence = screenshotPath == null
                    ? "Screenshot not available: driver does not support screenshots."
                    : screenshotPath;
            return ExecutionResult.success(
                    step,
                    executedBy,
                    "REPORT",
                    evidence,
                    "Manual screenshot captured."
            );
        } catch (RuntimeException exception) {
            String message = failureMessage(
                    "Failed to capture manual screenshot for step row " + step.getExcelRow() + ".",
                    step,
                    exception
            );
            return ExecutionResult.failure(step, executedBy, "REPORT", message);
        }
    }

    private ExecutionResult logResolvedResult(ExecutionResult result) {
        if (ExecutionResult.STATUS_SKIP.equals(result.getStatus())) {
            LOGGER.info(
                    "Keyword skipped. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, "
                            + "Object = {}, Application = {}, XPath = {}, Status = {}, Source = {}, Message = {}",
                    result.getScenarioNo(),
                    result.getScenarioAction(),
                    result.getTestcaseName(),
                    result.getExcelRowNumber(),
                    result.getKeywordName(),
                    result.getObjectName(),
                    result.getApplication(),
                    result.getResolvedXpath(),
                    result.getStatus(),
                    result.getExecutionSource(),
                    result.getMessage()
            );
        } else if (result.isSuccess()) {
            LOGGER.info(
                    "Completed keyword. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, "
                            + "Object = {}, Application = {}, XPath = {}, Status = {}, Source = {}",
                    result.getScenarioNo(),
                    result.getScenarioAction(),
                    result.getTestcaseName(),
                    result.getExcelRowNumber(),
                    result.getKeywordName(),
                    result.getObjectName(),
                    result.getApplication(),
                    result.getResolvedXpath(),
                    result.getStatus(),
                    result.getExecutionSource()
            );
        } else {
            LOGGER.error(
                    "Keyword failed. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, "
                            + "Object = {}, Application = {}, XPath = {}, Status = {}, Source = {}, Message = {}",
                    result.getScenarioNo(),
                    result.getScenarioAction(),
                    result.getTestcaseName(),
                    result.getExcelRowNumber(),
                    result.getKeywordName(),
                    result.getObjectName(),
                    result.getApplication(),
                    result.getResolvedXpath(),
                    result.getStatus(),
                    result.getExecutionSource(),
                    result.getMessage()
            );
        }
        return result;
    }

    private void logResolvedStepStarted(ResolvedStepContext step) {
        String valueForLog = SENSITIVE_DATA_MASKER.maskIfNeeded(
                step.getResolvedValue(),
                false,
                step.getRawValue(),
                step.getObjectName(),
                step.getResolvedXPath(),
                step.getDescription(),
                step.getKeyword()
        );
        LOGGER.info(
                "Executing keyword. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, "
                        + "Object = {}, Application = {}, XPath = {}, Value = {}",
                step.getScenarioNo(),
                step.getScenarioAction(),
                step.getTestcaseName(),
                step.getExcelRow(),
                step.getKeyword(),
                step.getObjectName(),
                step.getApplication(),
                step.getResolvedXPath(),
                valueForLog
        );
    }

    private boolean resolveValue(ExecutionContext context) {
        TestStep step = context.getTestStep();
        String rawValue = safe(step.getValue());
        try {
            context.setResolvedValue(dataReader.resolveValue(rawValue, context.getScenario()));
            return true;
        } catch (RuntimeException exception) {
            context.setExecutedBySource("DATA");
            context.setExecutedByClass(DataReader.class.getName());
            context.setResolvedValue("");
            context.setResolvedXpath("");
            context.setRawXpath("");
            context.setMessage("Failed to resolve value for step row " + step.getExcelRowNumber() + "."
                    + System.lineSeparator()
                    + "Raw value: " + rawValue + "."
                    + System.lineSeparator()
                    + stepContext(context)
                    + System.lineSeparator()
                    + "Cause: " + exception.getMessage());
            return false;
        }
    }

    private boolean resolveObject(ExecutionContext context) {
        TestStep step = context.getTestStep();
        if (isBlank(step.getObject())) {
            context.setRawXpath("");
            context.setResolvedXpath("");
            return true;
        }

        try {
            ResolvedObject resolvedObject = objectRepositoryReader.resolveObject(step, context.getScenario());
            context.setResolvedObject(resolvedObject);
            context.setRawXpath(resolvedObject == null ? "" : resolvedObject.getRawXpath());
            context.setResolvedXpath(resolvedObject == null ? "" : resolvedObject.getResolvedXpath());
            if (resolvedObject != null) {
                context.setResolvedValue(resolvedObject.getResolvedValue());
            }
            return true;
        } catch (RuntimeException exception) {
            context.setExecutedBySource("OBJECT");
            context.setExecutedByClass(ObjectRepositoryReader.class.getName());
            context.setMessage("Failed to resolve object for step row " + step.getExcelRowNumber() + "."
                    + System.lineSeparator()
                    + stepContext(context)
                    + System.lineSeparator()
                    + "Cause: " + exception.getMessage());
            return false;
        }
    }

    private ExecutionResult executeKeyword(ExecutionContext context) {
        TestStep step = context.getTestStep();
        StepContextHolder.set(resolvedStep(context));
        try {
            FunctionExecutionResult keywordResult = functionResolver.execute(
                    step.getApplication(),
                    step.getKeyword()
            );
            context.setExecutedByClass(keywordResult.getExecutedByClass());
            context.setExecutedBySource(keywordResult.getSourceType().name());
            return ExecutionResult.success(
                    context.getScenario(),
                    step,
                    context.getResolvedValue(),
                    context.getRawXpath(),
                    context.getResolvedXpath(),
                    context.getExecutedByClass(),
                    context.getExecutedBySource(),
                    keywordResult.getMessage()
            );
        } catch (RuntimeException | AssertionError exception) {
            context.setMessage("Keyword '" + safe(step.getKeyword()) + "' failed at step row "
                    + step.getExcelRowNumber() + "."
                    + System.lineSeparator()
                    + stepContext(context)
                    + System.lineSeparator()
                    + "Cause: " + exception.getMessage());
            return failureFromContext(context);
        } finally {
            StepContextHolder.clear();
        }
    }

    private ResolvedStepContext resolvedStep(ExecutionContext context) {
        Scenario scenario = context.getScenario();
        TestCaseBlock testcase = context.getTestCaseBlock();
        TestStep step = context.getTestStep();
        return ResolvedStepContext.builder()
                .scenarioNo(scenario == null ? safe(step.getScenarioNo()) : safe(scenario.getNo()))
                .scenarioAction(scenario == null ? safe(step.getScenarioAction()) : safe(scenario.getAction()))
                .scenarioName(scenario == null ? safe(step.getScenarioName()) : safe(scenario.getScenarioName()))
                .sheetName(scenario == null ? safe(step.getScenarioAction()) : safe(scenario.getAction()))
                .testcaseName(safe(step.getTestcaseName()))
                .testcaseParentRow(testcase == null ? 0 : testcase.getExcelRowNumber())
                .excelRow(step.getExcelRowNumber())
                .stepNumber(context.getCurrentStepNumber())
                .keyword(safe(step.getKeyword()))
                .objectName(safe(step.getObject()))
                .application(safe(step.getApplication()))
                .description(safe(step.getDescription()))
                .rawValue(safe(step.getValue()))
                .resolvedValue(context.getResolvedValue())
                .rawXPath(context.getRawXpath())
                .resolvedXPath(context.getResolvedXpath())
                .executedBy(context.getExecutedByClass())
                .build();
    }

    private ExecutionResult executeManualScreenshot(ExecutionContext context) {
        TestStep step = context.getTestStep();
        context.setExecutedByClass(KeywordEngine.class.getName());
        context.setExecutedBySource("REPORT");

        if (!reportConfig.isManualScreenshotEnabled()) {
            return ExecutionResult.skipped(
                    context.getScenario(),
                    step,
                    context.getResolvedValue(),
                    "",
                    "",
                    context.getExecutedByClass(),
                    context.getExecutedBySource(),
                    MANUAL_SCREENSHOT_DISABLED_MESSAGE,
                    MANUAL_SCREENSHOT_DISABLED_MESSAGE
            );
        }

        try {
            String label = isBlank(context.getResolvedValue()) ? "ManualScreenshot" : context.getResolvedValue();
            String screenshotPath = screenshotService.capture(driver, screenshotBaseName(context, label));
            String evidence = screenshotPath == null
                    ? "Screenshot not available: driver does not support screenshots."
                    : screenshotPath;
            return ExecutionResult.success(
                    context.getScenario(),
                    step,
                    context.getResolvedValue(),
                    "",
                    "",
                    context.getExecutedByClass(),
                    context.getExecutedBySource(),
                    evidence,
                    "Manual screenshot captured."
            );
        } catch (RuntimeException exception) {
            context.setMessage("Failed to capture manual screenshot for step row "
                    + step.getExcelRowNumber() + ". Cause: " + exception.getMessage());
            return failureFromContext(context);
        }
    }

    private ExecutionResult logManualScreenshot(ExecutionResult result) {
        if (ExecutionResult.STATUS_SKIP.equals(result.getStatus())) {
            LOGGER.info(
                    "Manual screenshot skipped. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Message = {}",
                    result.getScenarioNo(),
                    result.getScenarioAction(),
                    result.getTestcaseName(),
                    result.getExcelRowNumber(),
                    result.getMessage()
            );
        } else if (result.isSuccess()) {
            LOGGER.info(
                    "Manual screenshot captured. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Evidence = {}",
                    result.getScenarioNo(),
                    result.getScenarioAction(),
                    result.getTestcaseName(),
                    result.getExcelRowNumber(),
                    result.getEvidence()
            );
        } else {
            logFailure(result);
        }
        return result;
    }

    private void validateScenarioAndStep(Scenario scenario, TestStep step) {
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario must not be null.");
        }
        if (step == null) {
            throw new IllegalArgumentException("TestStep must not be null.");
        }
    }

    private ExecutionResult failure(ExecutionContext context, String message) {
        context.setMessage(message);
        return failureFromContext(context);
    }

    private ExecutionResult failureFromContext(ExecutionContext context) {
        TestStep step = context.getTestStep();
        return ExecutionResult.failure(
                context.getScenario(),
                step,
                context.getResolvedValue(),
                context.getRawXpath(),
                context.getResolvedXpath(),
                context.getExecutedByClass(),
                context.getExecutedBySource(),
                context.getMessage()
        );
    }

    private ExecutionResult logFailure(ExecutionResult result) {
        LOGGER.error(
                "Step failed. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Keyword = {}, Message = {}",
                result.getScenarioNo(),
                result.getScenarioAction(),
                result.getTestcaseName(),
                result.getExcelRowNumber(),
                result.getKeywordName(),
                result.getMessage()
        );
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String stepContext(ExecutionContext context) {
        Scenario scenario = context.getScenario();
        TestStep step = context.getTestStep();
        return new ErrorContext()
                .scenarioNo(scenario == null ? "" : scenario.getNo())
                .scenarioAction(scenario == null ? "" : scenario.getAction())
                .sheet(step == null ? "" : step.getScenarioAction())
                .testcase(step == null ? "" : step.getTestcaseName())
                .row(step == null ? 0 : step.getExcelRowNumber())
                .keyword(step == null ? "" : step.getKeyword())
                .object(step == null ? "" : step.getObject())
                .application(step == null ? "" : step.getApplication())
                .render();
    }

    private String resolvedStepContext(ResolvedStepContext step) {
        return new ErrorContext()
                .scenarioNo(step.getScenarioNo())
                .scenarioAction(step.getScenarioAction())
                .sheet(step.getSheetName())
                .testcase(step.getTestcaseName())
                .row(step.getExcelRow())
                .keyword(step.getKeyword())
                .object(step.getObjectName())
                .application(step.getApplication())
                .render();
    }

    private String failureMessage(String summary, ResolvedStepContext step, Throwable cause) {
        String message = summary + System.lineSeparator() + resolvedStepContext(step);
        if (cause != null) {
            message += System.lineSeparator() + "Cause: " + safe(cause.getMessage());
        }
        return message;
    }

    private boolean isScreenshotKeyword(String keywordName) {
        return keywordName != null && "screenshot".equalsIgnoreCase(keywordName.trim());
    }

    private String screenshotBaseName(ExecutionContext context, String label) {
        TestStep step = context.getTestStep();
        return String.join(
                "_",
                safe(context.getScenario().getNo()),
                safe(step.getTestcaseName()),
                "step" + step.getStepOrder(),
                "row" + step.getExcelRowNumber(),
                safe(label)
        );
    }

    private String screenshotBaseName(ResolvedStepContext step, String label) {
        return String.join(
                "_",
                safe(step.getScenarioNo()),
                safe(step.getTestcaseName()),
                "step" + step.getStepNumber(),
                "row" + step.getExcelRow(),
                safe(label)
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean containsStepContext(String message) {
        return message != null
                && (message.contains("Scenario NO:") || message.contains("Scenario ACTION:"));
    }
}
