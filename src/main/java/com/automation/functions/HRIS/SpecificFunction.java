package com.automation.functions.HRIS;

import com.automation.base.BaseFunction;
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

    public void verifyEmployeeVisible(String xpath, String value) {
        validateRequired(xpath, "XPath", "verifyEmployeeVisible");
        validateRequired(value, "Value", "verifyEmployeeVisible");
        LOGGER.info("Executing HRIS-specific employee visibility placeholder for value: {}", value);
        verifyTextContains(xpath, value);
    }

    private void validateRequired(String value, String fieldName, String keyword) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required for keyword '" + keyword + "'.");
        }
    }
}
