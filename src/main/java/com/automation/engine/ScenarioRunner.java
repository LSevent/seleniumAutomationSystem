package com.automation.engine;

import com.automation.excel.DataReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ExecutionResult;
import com.automation.models.ResolvedScenarioContext;
import com.automation.models.ResolvedStepContext;
import com.automation.models.ResolvedTestcaseContext;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.models.TestStep;
import com.automation.reports.ExcelExecutionReporter;
import com.automation.validation.PreRunValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ScenarioRunner {

    private static final Logger LOGGER = LogManager.getLogger(ScenarioRunner.class);

    private final ScenarioReader scenarioReader;
    private final StepReader stepReader;
    private KeywordEngine keywordEngine;
    private final Supplier<KeywordEngine> keywordEngineSupplier;
    private final ExcelExecutionReporter reporter;
    private final ExecutionPlanBuilder executionPlanBuilder;
    private final PreRunValidator preRunValidator;

    public ScenarioRunner(
            ScenarioReader scenarioReader,
            StepReader stepReader,
            KeywordEngine keywordEngine
    ) {
        this(scenarioReader, stepReader, keywordEngine, null);
    }

    public ScenarioRunner(
            ScenarioReader scenarioReader,
            StepReader stepReader,
            KeywordEngine keywordEngine,
            ExcelExecutionReporter reporter
    ) {
        if (scenarioReader == null) {
            throw new IllegalArgumentException("ScenarioReader must not be null.");
        }
        if (stepReader == null) {
            throw new IllegalArgumentException("StepReader must not be null.");
        }
        if (keywordEngine == null) {
            throw new IllegalArgumentException("KeywordEngine must not be null.");
        }
        this.scenarioReader = scenarioReader;
        this.stepReader = stepReader;
        this.keywordEngine = keywordEngine;
        this.keywordEngineSupplier = () -> keywordEngine;
        this.reporter = reporter;
        this.executionPlanBuilder = new ExecutionPlanBuilder(
                scenarioReader,
                stepReader,
                keywordEngine.getDataReader(),
                keywordEngine.getObjectRepositoryReader()
        );
        this.preRunValidator = new PreRunValidator();
    }

    public ScenarioRunner(
            ScenarioReader scenarioReader,
            StepReader stepReader,
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            Supplier<KeywordEngine> keywordEngineSupplier
    ) {
        if (scenarioReader == null) {
            throw new IllegalArgumentException("ScenarioReader must not be null.");
        }
        if (stepReader == null) {
            throw new IllegalArgumentException("StepReader must not be null.");
        }
        if (dataReader == null) {
            throw new IllegalArgumentException("DataReader must not be null.");
        }
        if (objectRepositoryReader == null) {
            throw new IllegalArgumentException("ObjectRepositoryReader must not be null.");
        }
        if (keywordEngineSupplier == null) {
            throw new IllegalArgumentException("KeywordEngine supplier must not be null.");
        }
        this.scenarioReader = scenarioReader;
        this.stepReader = stepReader;
        this.keywordEngineSupplier = keywordEngineSupplier;
        this.reporter = null;
        this.executionPlanBuilder = new ExecutionPlanBuilder(
                scenarioReader,
                stepReader,
                dataReader,
                objectRepositoryReader
        );
        this.preRunValidator = new PreRunValidator();
    }

    public List<ExecutionResult> runActiveScenarios() {
        List<ExecutionResult> results = new ArrayList<>();
        try {
            List<ResolvedScenarioContext> executionPlan = executionPlanBuilder.build();
            preRunValidator.validate(executionPlan);
            for (ResolvedScenarioContext scenario : executionPlan) {
                List<ExecutionResult> scenarioResults = runResolvedScenario(scenario);
                results.addAll(scenarioResults);
                if (scenarioResults.stream().anyMatch(result -> !result.isSuccess())) {
                    break;
                }
            }
            return results;
        } finally {
            flushReporter();
        }
    }

    private List<ExecutionResult> runResolvedScenario(ResolvedScenarioContext resolvedScenario) {
        Scenario scenario = scenarioFrom(resolvedScenario);
        LOGGER.info("Resolved scenario started. NO = {}, ACTION = {}", scenario.getNo(), scenario.getAction());
        startScenarioReport(scenario);

        List<ExecutionResult> results = new ArrayList<>();
        for (ResolvedTestcaseContext resolvedTestcase : resolvedScenario.getTestcases()) {
            TestCaseBlock testcase = testcaseFrom(resolvedScenario, resolvedTestcase);
            LOGGER.info(
                    "Resolved testcase started. Scenario NO = {}, ACTION = {}, Testcase = {}",
                    scenario.getNo(),
                    scenario.getAction(),
                    testcase.getTestcaseName()
            );
            startTestcaseReport(testcase);

            for (ResolvedStepContext step : resolvedTestcase.getSteps()) {
                ExecutionResult result = keywordEngine().execute(step);
                results.add(result);
                logStepReport(result);
                if (!result.isSuccess()) {
                    String scenarioMessage = "Scenario failed. NO = " + scenario.getNo()
                            + ", ACTION = " + scenario.getAction()
                            + ", Testcase = " + testcase.getTestcaseName()
                            + ". " + result.getMessage();
                    LOGGER.error(
                            "Resolved scenario failed. NO = {}, ACTION = {}, Testcase = {}. Message = {}",
                            scenario.getNo(),
                            scenario.getAction(),
                            testcase.getTestcaseName(),
                            result.getMessage()
                    );
                    finishTestcaseReport(testcase, false, result.getMessage());
                    finishScenarioReport(scenario, false, scenarioMessage);
                    return results;
                }
            }

            finishTestcaseReport(testcase, true, "");
            LOGGER.info(
                    "Resolved testcase finished. Scenario NO = {}, ACTION = {}, Testcase = {}",
                    scenario.getNo(),
                    scenario.getAction(),
                    testcase.getTestcaseName()
            );
        }

        LOGGER.info("Resolved scenario finished. NO = {}, ACTION = {}", scenario.getNo(), scenario.getAction());
        finishScenarioReport(scenario, true, "");
        return results;
    }

    private Scenario scenarioFrom(ResolvedScenarioContext scenario) {
        return new Scenario(
                scenario.getScenarioNo(),
                true,
                scenario.getScenarioAction(),
                scenario.getScenarioName(),
                0
        );
    }

    private TestCaseBlock testcaseFrom(
            ResolvedScenarioContext scenario,
            ResolvedTestcaseContext testcase
    ) {
        return new TestCaseBlock(
                scenario.getScenarioNo(),
                scenario.getScenarioName(),
                scenario.getScenarioAction(),
                testcase.getTestcaseName(),
                true,
                testcase.getApplication(),
                "",
                testcase.getParentExcelRow()
        );
    }

    public List<ExecutionResult> runScenario(Scenario scenario) {
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario must not be null.");
        }

        LOGGER.info("Scenario started. NO = {}, ACTION = {}", scenario.getNo(), scenario.getAction());
        startScenarioReport(scenario);
        List<ExecutionResult> results = new ArrayList<>();
        boolean scenarioSuccess = true;
        String scenarioMessage = "";
        List<TestCaseBlock> activeTestCases = stepReader.getActiveTestCases(scenario);

        if (activeTestCases.isEmpty()) {
            String message = "Active scenario has no active testcase. Scenario NO = "
                    + scenario.getNo() + ", ACTION = " + scenario.getAction() + ".";
            finishScenarioReport(scenario, false, message);
            throw new FrameworkException(message);
        }

        for (TestCaseBlock testCaseBlock : activeTestCases) {
            LOGGER.info(
                    "Testcase started. Scenario NO = {}, ACTION = {}, Testcase = {}",
                    scenario.getNo(),
                    scenario.getAction(),
                    testCaseBlock.getTestcaseName()
            );
            startTestcaseReport(testCaseBlock);

            boolean testcaseSuccess = true;
            String testcaseMessage = "";
            for (TestStep step : testCaseBlock.getSteps()) {
                ExecutionResult result = keywordEngine().executeStep(scenario, step);
                results.add(result);
                logStepReport(result);
                if (!result.isSuccess()) {
                    scenarioSuccess = false;
                    testcaseSuccess = false;
                    testcaseMessage = result.getMessage();
                    scenarioMessage = "Scenario failed. NO = " + scenario.getNo()
                            + ", ACTION = " + scenario.getAction()
                            + ", Testcase = " + testCaseBlock.getTestcaseName()
                            + ". " + result.getMessage();
                    LOGGER.error(
                            "Scenario failed. NO = {}, ACTION = {}, Testcase = {}. Message = {}",
                            scenario.getNo(),
                            scenario.getAction(),
                            testCaseBlock.getTestcaseName(),
                            result.getMessage()
                    );
                    finishTestcaseReport(testCaseBlock, testcaseSuccess, testcaseMessage);
                    finishScenarioReport(scenario, scenarioSuccess, scenarioMessage);
                    return results;
                }
            }
            finishTestcaseReport(testCaseBlock, testcaseSuccess, testcaseMessage);

            LOGGER.info(
                    "Testcase finished. Scenario NO = {}, ACTION = {}, Testcase = {}",
                    scenario.getNo(),
                    scenario.getAction(),
                    testCaseBlock.getTestcaseName()
            );
        }

        LOGGER.info("Scenario finished. NO = {}, ACTION = {}", scenario.getNo(), scenario.getAction());
        finishScenarioReport(scenario, scenarioSuccess, scenarioMessage);
        return results;
    }

    private void startScenarioReport(Scenario scenario) {
        if (reporter != null) {
            reporter.startScenario(scenario);
        }
    }

    private void finishScenarioReport(Scenario scenario, boolean success, String message) {
        if (reporter != null) {
            reporter.finishScenario(scenario, success, message);
        }
    }

    private void startTestcaseReport(TestCaseBlock testCaseBlock) {
        if (reporter != null) {
            reporter.startTestCase(testCaseBlock);
        }
    }

    private void finishTestcaseReport(TestCaseBlock testCaseBlock, boolean success, String message) {
        if (reporter != null) {
            reporter.finishTestCase(testCaseBlock, success, message);
        }
    }

    private void logStepReport(ExecutionResult result) {
        if (reporter != null) {
            reporter.logStep(result);
        }
    }

    private void flushReporter() {
        if (reporter != null) {
            reporter.flush();
        }
    }

    private KeywordEngine keywordEngine() {
        if (keywordEngine == null) {
            keywordEngine = keywordEngineSupplier.get();
            if (keywordEngine == null) {
                throw new IllegalStateException("KeywordEngine supplier returned null.");
            }
        }
        return keywordEngine;
    }
}
