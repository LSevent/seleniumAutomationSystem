package com.automation.engine;

import com.automation.context.EvidenceContextHolder;
import com.automation.context.ScreenshotContextHolder;
import com.automation.context.StepContextHolder;
import com.automation.excel.DataReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.exceptions.ErrorContext;
import com.automation.config.ExcelExecutionConfig;
import com.automation.models.ExecutionResult;
import com.automation.models.KeywordExecutionResult;
import com.automation.models.ResolvedStepContext;
import com.automation.reports.ExcelReportConfig;
import com.automation.reports.SensitiveDataMasker;
import com.automation.services.ScreenshotService;
import com.automation.utils.WaitUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class KeywordEngine {

    private static final Logger LOGGER = LogManager.getLogger(KeywordEngine.class);
    private static final SensitiveDataMasker SENSITIVE_DATA_MASKER = new SensitiveDataMasker();

    private final DataReader dataReader;
    private final ObjectRepositoryReader objectRepositoryReader;
    private final KeywordResolver keywordResolver;
    private final ExcelExecutionConfig executionConfig;
    private final ExcelReportConfig reportConfig;
    private final ScreenshotService screenshotService;

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            KeywordResolver keywordResolver
    ) {
        this(dataReader, objectRepositoryReader, keywordResolver, ExcelReportConfig.fromConfig());
    }

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            KeywordResolver keywordResolver,
            ExcelReportConfig reportConfig
    ) {
        this(dataReader, objectRepositoryReader, keywordResolver, reportConfig, ExcelExecutionConfig.load());
    }

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            KeywordResolver keywordResolver,
            ExcelReportConfig reportConfig,
            ExcelExecutionConfig executionConfig
    ) {
        this(dataReader, objectRepositoryReader, keywordResolver, reportConfig, executionConfig, null);
    }

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            KeywordResolver keywordResolver,
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
        if (keywordResolver == null) {
            throw new IllegalArgumentException("KeywordResolver must not be null.");
        }
        this.dataReader = dataReader;
        this.objectRepositoryReader = objectRepositoryReader;
        this.keywordResolver = keywordResolver;
        ExcelReportConfig resolvedReportConfig = reportConfig == null ? ExcelReportConfig.fromConfig() : reportConfig;
        this.executionConfig = executionConfig == null ? ExcelExecutionConfig.load() : executionConfig;
        ScreenshotService resolvedScreenshotService = screenshotService == null
                ? new ScreenshotService(this.executionConfig.getScreenshotOutputDirectory())
                : screenshotService;
        this.reportConfig = resolvedReportConfig;
        this.screenshotService = resolvedScreenshotService;
    }

    public ExecutionResult execute(ResolvedStepContext step) {
        if (step == null) {
            throw new IllegalArgumentException("ResolvedStepContext must not be null.");
        }

        StepContextHolder.set(step);
        EvidenceContextHolder.start();
        ScreenshotContextHolder.set(screenshotService, reportConfig);
        WaitUtil.setTimeoutSeconds(executionConfig.getTimeoutSeconds());
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
            } else {
                result = executeResolvedKeyword(step);
            }
            result = attachCollectedEvidence(result);
            return logResolvedResult(result);
        } finally {
            WaitUtil.clearTimeoutSeconds();
            ScreenshotContextHolder.clear();
            EvidenceContextHolder.clear();
            StepContextHolder.clear();
        }
    }

    DataReader getDataReader() {
        return dataReader;
    }

    ObjectRepositoryReader getObjectRepositoryReader() {
        return objectRepositoryReader;
    }

    private ExecutionResult executeResolvedKeyword(ResolvedStepContext step) {
        try {
            KeywordExecutionResult keywordResult = keywordResolver.execute(
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

    private ExecutionResult attachCollectedEvidence(ExecutionResult result) {
        List<String> collectedEvidence = EvidenceContextHolder.getAll();
        if (collectedEvidence.isEmpty()) {
            return result;
        }

        String mergedEvidence = mergeEvidence(result.getEvidence(), collectedEvidence);
        if (mergedEvidence.equals(safe(result.getEvidence()))) {
            return result;
        }

        return ExecutionResult.builder()
                .scenarioNo(result.getScenarioNo())
                .scenarioName(result.getScenarioName())
                .scenarioAction(result.getScenarioAction())
                .testcaseName(result.getTestcaseName())
                .description(result.getDescription())
                .keywordName(result.getKeywordName())
                .objectName(result.getObjectName())
                .application(result.getApplication())
                .rawValue(result.getRawValue())
                .resolvedValue(result.getResolvedValue())
                .rawXPath(result.getRawXPath())
                .resolvedXPath(result.getResolvedXPath())
                .executedByClass(result.getExecutedByClass())
                .executionSource(result.getExecutionSource())
                .success(result.isSuccess())
                .status(result.getStatus())
                .evidence(mergedEvidence)
                .message(result.getMessage())
                .excelRowNumber(result.getExcelRowNumber())
                .stepOrder(result.getStepOrder())
                .build();
    }

    private String mergeEvidence(String existingEvidence, List<String> collectedEvidence) {
        Set<String> evidenceItems = new LinkedHashSet<>();
        addEvidenceItems(evidenceItems, existingEvidence);
        collectedEvidence.forEach(evidence -> addEvidenceItems(evidenceItems, evidence));
        return String.join(System.lineSeparator(), evidenceItems);
    }

    private void addEvidenceItems(Set<String> evidenceItems, String evidence) {
        if (isBlank(evidence)) {
            return;
        }
        for (String item : evidence.split("\\R")) {
            if (!isBlank(item)) {
                evidenceItems.add(item.trim());
            }
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
                    result.getResolvedXPath(),
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
                    result.getResolvedXPath(),
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
                    result.getResolvedXPath(),
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean containsStepContext(String message) {
        return message != null
                && (message.contains("Scenario NO:") || message.contains("Scenario ACTION:"));
    }
}
