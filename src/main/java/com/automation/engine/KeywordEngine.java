package com.automation.engine;

import com.automation.excel.DataReader;
import com.automation.excel.ObjectRepositoryReader;
import com.automation.models.ExecutionResult;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.ResolvedObject;
import com.automation.models.Scenario;
import com.automation.models.TestStep;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class KeywordEngine {

    private static final Logger LOGGER = LogManager.getLogger(KeywordEngine.class);

    private final DataReader dataReader;
    private final ObjectRepositoryReader objectRepositoryReader;
    private final FunctionResolver functionResolver;

    public KeywordEngine(
            DataReader dataReader,
            ObjectRepositoryReader objectRepositoryReader,
            FunctionResolver functionResolver
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
    }

    public ExecutionResult executeStep(Scenario scenario, TestStep step) {
        validateScenarioAndStep(scenario, step);

        ExecutionContext context = new ExecutionContext();
        context.setScenario(scenario);
        context.setTestStep(step);
        context.setCurrentStepNumber(step.getStepOrder());

        LOGGER.info(
                "Step started. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Function = {}, Object = {}",
                scenario.getNo(),
                scenario.getAction(),
                step.getTestcaseName(),
                step.getExcelRowNumber(),
                step.getFunction(),
                step.getObject()
        );

        if (isBlank(step.getFunction())) {
            return logFailure(failure(context, "Function is required in sheet " + scenario.getAction() + " row " + step.getExcelRowNumber() + "."));
        }

        if (!resolveValue(context)) {
            return logFailure(failureFromContext(context));
        }
        if (!resolveObject(context)) {
            return logFailure(failureFromContext(context));
        }

        ExecutionResult result = executeFunction(context);
        if (result.isSuccess()) {
            LOGGER.info(
                    "Step passed. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Function = {}, Source = {}",
                    scenario.getNo(),
                    scenario.getAction(),
                    step.getTestcaseName(),
                    step.getExcelRowNumber(),
                    step.getFunction(),
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
            context.setMessage("Failed to resolve value for step row " + step.getExcelRowNumber()
                    + ". Raw value: " + rawValue + ". Cause: " + exception.getMessage());
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
            context.setMessage("Failed to resolve object for step row " + step.getExcelRowNumber()
                    + ". Application: " + safe(step.getApplication())
                    + ", Object: " + safe(step.getObject())
                    + ". Cause: " + exception.getMessage());
            return false;
        }
    }

    private ExecutionResult executeFunction(ExecutionContext context) {
        TestStep step = context.getTestStep();
        try {
            FunctionExecutionResult functionResult = functionResolver.execute(
                    step.getApplication(),
                    step.getFunction(),
                    context.getResolvedXpath(),
                    context.getResolvedValue()
            );
            context.setExecutedByClass(functionResult.getExecutedByClass());
            context.setExecutedBySource(functionResult.getSourceType().name());
            return ExecutionResult.success(
                    context.getScenario(),
                    step,
                    context.getResolvedValue(),
                    context.getRawXpath(),
                    context.getResolvedXpath(),
                    context.getExecutedByClass(),
                    context.getExecutedBySource(),
                    functionResult.getMessage()
            );
        } catch (RuntimeException | AssertionError exception) {
            context.setMessage("Failed to execute keyword '" + safe(step.getFunction()) + "' for step row "
                    + step.getExcelRowNumber() + ". Cause: " + exception.getMessage());
            return failureFromContext(context);
        }
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
                "Step failed. Scenario NO = {}, ACTION = {}, Testcase = {}, Row = {}, Function = {}, Message = {}",
                result.getScenarioNo(),
                result.getScenarioAction(),
                result.getTestcaseName(),
                result.getExcelRowNumber(),
                result.getFunctionName(),
                result.getMessage()
        );
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
