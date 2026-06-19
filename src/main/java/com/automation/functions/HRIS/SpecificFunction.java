package com.automation.functions.HRIS;

import com.automation.base.BaseFunction;
import com.automation.context.StepContextHolder;
import com.automation.models.ResolvedStepContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

public class SpecificFunction extends BaseFunction {

    private static final Logger LOGGER = LogManager.getLogger(SpecificFunction.class);

    public SpecificFunction(WebDriver driver) {
        super(driver);
    }

    public void waitForApplicationReady() {
        ResolvedStepContext step = StepContextHolder.get();
        LOGGER.info(
                "Executing HRIS application readiness placeholder. Scenario: {}, Testcase: {}, Row: {}",
                step.getScenarioNo(),
                step.getTestcaseName(),
                step.getExcelRow()
        );
    }

    public void verifyEmployeeVisible() {
        ResolvedStepContext step = StepContextHolder.get();
        executeVerifyEmployeeVisible(step.getResolvedXPath(), step.getResolvedValue(), step);
    }

    private void executeVerifyEmployeeVisible(String xpath, String value, ResolvedStepContext step) {
        validateRequired(xpath, "verifyEmployeeVisible", "XPath", step);
        validateRequired(value, "verifyEmployeeVisible", "Value", step);
        LOGGER.info("Executing HRIS-specific employee visibility placeholder for value: {}", value);
        executeVerifyTextContains(xpath, value, step);
    }
}
