package com.automation.functions.BRS;

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
        LOGGER.info("Executing BRS application readiness placeholder.");
    }

    @Override
    public void click(String xpath) {
        LOGGER.info("Executing BRS-specific click override.");
        super.click(xpath);
    }

    public void selectRoomByName(String xpath, String value) {
        validateRequired(xpath, "XPath", "selectRoomByName");
        validateRequired(value, "Value", "selectRoomByName");
        LOGGER.info("Executing BRS-specific room selection placeholder for value: {}", value);
        safeClick(xpath);
    }

    public void verifyBookingCreated(String xpath, String expectedMessage) {
        validateRequired(xpath, "XPath", "verifyBookingCreated");
        validateRequired(expectedMessage, "Value", "verifyBookingCreated");
        LOGGER.info("Executing BRS-specific booking verification placeholder.");
        verifyTextContains(xpath, expectedMessage);
    }

    private void validateRequired(String value, String fieldName, String keyword) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required for keyword '" + keyword + "'.");
        }
    }
}
