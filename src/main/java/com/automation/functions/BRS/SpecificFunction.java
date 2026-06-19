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
        ResolvedStepContext step = StepContextHolder.get();
        LOGGER.info(
                "Executing BRS application readiness placeholder. Scenario: {}, Testcase: {}, Row: {}",
                step.getScenarioNo(),
                step.getTestcaseName(),
                step.getExcelRow()
        );
    }

    @Override
    public void click() {
        ResolvedStepContext step = StepContextHolder.get();
        LOGGER.info("Executing BRS-specific click using resolved step context.");
        executeClick(step.getResolvedXPath(), step);
    }

    public void selectRoomByName() {
        ResolvedStepContext step = StepContextHolder.get();
        executeSelectRoomByName(step.getResolvedXPath(), step.getResolvedValue(), step);
    }

    private void executeSelectRoomByName(String xpath, String value, ResolvedStepContext step) {
        validateRequired(xpath, "selectRoomByName", "XPath", step);
        validateRequired(value, "selectRoomByName", "Value", step);
        LOGGER.info("Executing BRS-specific room selection placeholder for value: {}", value);
        executeSafeClick(xpath, step);
    }

    public void verifyBookingCreated() {
        ResolvedStepContext step = StepContextHolder.get();
        executeVerifyBookingCreated(step.getResolvedXPath(), step.getResolvedValue(), step);
    }

    private void executeVerifyBookingCreated(
            String xpath,
            String expectedMessage,
            ResolvedStepContext step
    ) {
        validateRequired(xpath, "verifyBookingCreated", "XPath", step);
        validateRequired(expectedMessage, "verifyBookingCreated", "Value", step);
        LOGGER.info("Executing BRS-specific booking verification placeholder.");
        executeVerifyTextContains(xpath, expectedMessage, step);
    }
}
