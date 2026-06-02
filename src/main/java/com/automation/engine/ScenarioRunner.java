package com.automation.engine;

import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.ExecutionResult;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import com.automation.models.TestStep;
import com.automation.reports.ExcelExecutionReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ScenarioRunner {

    private static final Logger LOGGER = LogManager.getLogger(ScenarioRunner.class);

    private final ScenarioReader scenarioReader;
    private final StepReader stepReader;
    private final KeywordEngine keywordEngine;
    private final ExcelExecutionReporter reporter;

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
        this.reporter = reporter;
    }

    public List<ExecutionResult> runActiveScenarios() {
        List<ExecutionResult> results = new ArrayList<>();
        try {
            for (Scenario scenario : scenarioReader.getActiveScenarios()) {
                List<ExecutionResult> scenarioResults = runScenario(scenario);
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

    public List<ExecutionResult> runScenario(Scenario scenario) {
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario must not be null.");
        }

        LOGGER.info("Scenario started. NO = {}, ACTION = {}", scenario.getNo(), scenario.getAction());
        startScenarioReport(scenario);
        List<ExecutionResult> results = new ArrayList<>();
        boolean scenarioSuccess = true;
        String scenarioMessage = "";

        for (TestCaseBlock testCaseBlock : stepReader.getActiveTestCases(scenario)) {
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
                ExecutionResult result = keywordEngine.executeStep(scenario, step);
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
}
