package com.automation.functions.HRIS;

import com.automation.base.BaseFunction;
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
        ResolvedStepContext step = currentStep();
        LOGGER.info(
                "Executing HRIS application readiness placeholder. Scenario: {}, Testcase: {}, Row: {}",
                step.getScenarioNo(),
                step.getTestcaseName(),
                step.getExcelRow()
        );
    }

    public void verifyEmployeeVisible() {
        ResolvedStepContext step = currentStep();
        requireXPath(step, "verifyEmployeeVisible");
        String value = requireValue(step, "verifyEmployeeVisible", "Value");
        LOGGER.info("Executing HRIS-specific employee visibility placeholder for value: {}", value);
        verifyTextContains();
    }
}
