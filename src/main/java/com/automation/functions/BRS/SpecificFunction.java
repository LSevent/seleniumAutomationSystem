package com.automation.functions.BRS;

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
                "Executing BRS application readiness placeholder. Scenario: {}, Testcase: {}, Row: {}",
                step.getScenarioNo(),
                step.getTestcaseName(),
                step.getExcelRow()
        );
    }

    @Override
    public void click() {
        LOGGER.info("Executing BRS-specific click using resolved step context.");
        super.click();
    }

    public void selectRoomByName() {
        ResolvedStepContext step = currentStep();
        requireXPath(step, "selectRoomByName");
        String value = requireValue(step, "selectRoomByName", "Value");
        LOGGER.info("Executing BRS-specific room selection placeholder for value: {}", value);
        safeClick();
    }

    public void verifyBookingCreated() {
        ResolvedStepContext step = currentStep();
        requireXPath(step, "verifyBookingCreated");
        requireValue(step, "verifyBookingCreated", "Value");
        LOGGER.info("Executing BRS-specific booking verification placeholder.");
        verifyTextContains();
    }
}
