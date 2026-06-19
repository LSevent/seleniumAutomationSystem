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
        LOGGER.info("Executing HRIS application readiness placeholder.");
    }

    public void verifyEmployeeVisible() {
        ResolvedStepContext step = StepContextHolder.get();
        verifyEmployeeVisible(step.getResolvedXPath(), step.getResolvedValue(), step);
    }

    public void verifyEmployeeVisible(String xpath, String value) {
        verifyEmployeeVisible(xpath, value, currentContext());
    }

    private void verifyEmployeeVisible(String xpath, String value, ResolvedStepContext step) {
        validateRequired(xpath, "verifyEmployeeVisible", "XPath", step);
        validateRequired(value, "verifyEmployeeVisible", "Value", step);
        LOGGER.info("Executing HRIS-specific employee visibility placeholder for value: {}", value);
        verifyTextContains(xpath, value);
    }
}
