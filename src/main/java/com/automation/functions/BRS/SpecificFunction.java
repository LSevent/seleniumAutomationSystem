package com.automation.functions.BRS;

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
        LOGGER.info("Executing BRS application readiness placeholder.");
    }

    @Override
    public void click() {
        ResolvedStepContext step = StepContextHolder.get();
        LOGGER.info("Executing BRS-specific click using resolved step context.");
        super.click(step.getResolvedXPath(), step);
    }

    @Override
    public void click(String xpath) {
        LOGGER.info("Executing BRS-specific click override.");
        super.click(xpath);
    }

    public void selectRoomByName() {
        ResolvedStepContext step = StepContextHolder.get();
        selectRoomByName(step.getResolvedXPath(), step.getResolvedValue(), step);
    }

    public void selectRoomByName(String xpath, String value) {
        selectRoomByName(xpath, value, currentContext());
    }

    private void selectRoomByName(String xpath, String value, ResolvedStepContext step) {
        validateRequired(xpath, "selectRoomByName", "XPath", step);
        validateRequired(value, "selectRoomByName", "Value", step);
        LOGGER.info("Executing BRS-specific room selection placeholder for value: {}", value);
        safeClick(xpath);
    }

    public void verifyBookingCreated() {
        ResolvedStepContext step = StepContextHolder.get();
        verifyBookingCreated(step.getResolvedXPath(), step.getResolvedValue(), step);
    }

    public void verifyBookingCreated(String xpath, String expectedMessage) {
        verifyBookingCreated(xpath, expectedMessage, currentContext());
    }

    private void verifyBookingCreated(
            String xpath,
            String expectedMessage,
            ResolvedStepContext step
    ) {
        validateRequired(xpath, "verifyBookingCreated", "XPath", step);
        validateRequired(expectedMessage, "verifyBookingCreated", "Value", step);
        LOGGER.info("Executing BRS-specific booking verification placeholder.");
        verifyTextContains(xpath, expectedMessage);
    }
}
