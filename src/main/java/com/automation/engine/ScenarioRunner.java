package com.automation.engine;

import com.automation.excel.ScenarioReader;
import com.automation.excel.StepReader;
import com.automation.models.ExecutionResult;
import com.automation.models.Scenario;
import com.automation.models.TestCaseBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ScenarioRunner {

    private static final Logger LOGGER = LogManager.getLogger(ScenarioRunner.class);

    private final ScenarioReader scenarioReader;
    private final StepReader stepReader;
    private final KeywordEngine keywordEngine;

    public ScenarioRunner(
            ScenarioReader scenarioReader,
            StepReader stepReader,
            KeywordEngine keywordEngine
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
    }

    public List<ExecutionResult> runActiveScenarios() {
        List<ExecutionResult> results = new ArrayList<>();
        for (Scenario scenario : scenarioReader.getActiveScenarios()) {
            List<ExecutionResult> scenarioResults = runScenario(scenario);
            results.addAll(scenarioResults);
            if (scenarioResults.stream().anyMatch(result -> !result.isSuccess())) {
                break;
            }
        }
        return results;
    }

    public List<ExecutionResult> runScenario(Scenario scenario) {
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario must not be null.");
        }

        LOGGER.info("Scenario started. NO = {}, ACTION = {}", scenario.getNo(), scenario.getAction());
        List<ExecutionResult> results = new ArrayList<>();

        for (TestCaseBlock testCaseBlock : stepReader.getActiveTestCases(scenario)) {
            LOGGER.info(
                    "Testcase started. Scenario NO = {}, ACTION = {}, Testcase = {}",
                    scenario.getNo(),
                    scenario.getAction(),
                    testCaseBlock.getTestcaseName()
            );

            for (ExecutionResult result : keywordEngine.executeSteps(scenario, testCaseBlock.getSteps())) {
                results.add(result);
                if (!result.isSuccess()) {
                    LOGGER.error(
                            "Scenario failed. NO = {}, ACTION = {}, Testcase = {}. Message = {}",
                            scenario.getNo(),
                            scenario.getAction(),
                            testCaseBlock.getTestcaseName(),
                            result.getMessage()
                    );
                    return results;
                }
            }

            LOGGER.info(
                    "Testcase finished. Scenario NO = {}, ACTION = {}, Testcase = {}",
                    scenario.getNo(),
                    scenario.getAction(),
                    testCaseBlock.getTestcaseName()
            );
        }

        LOGGER.info("Scenario finished. NO = {}, ACTION = {}", scenario.getNo(), scenario.getAction());
        return results;
    }
}
