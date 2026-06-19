package com.automation.functions.BRS;

import com.automation.base.BaseFunction;
import com.automation.models.ResolvedStepContext;
import org.openqa.selenium.WebDriver;

public class SpecificFunction extends BaseFunction {

    public SpecificFunction(WebDriver driver) {
        super(driver);
    }

    public void waitForApplicationReady() {
        currentStep();
    }

    @Override
    public void click() {
        super.click();
    }

    public void selectRoomByName() {
        ResolvedStepContext step = currentStep();
        requireXPath(step, "selectRoomByName");
        requireValue(step, "selectRoomByName", "Value");
        safeClick();
    }

    public void verifyBookingCreated() {
        ResolvedStepContext step = currentStep();
        requireXPath(step, "verifyBookingCreated");
        requireValue(step, "verifyBookingCreated", "Value");
        verifyTextContains();
    }
}
