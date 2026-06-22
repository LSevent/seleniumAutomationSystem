package com.automation.functions.BRS;

import com.automation.base.BaseFunction;
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
        requiredXPath("selectRoomByName");
        requiredValue("selectRoomByName", "Value");
        safeClick();
    }

    public void verifyBookingCreated() {
        requiredXPath("verifyBookingCreated");
        requiredValue("verifyBookingCreated", "Value");
        verifyTextContains();
    }
}
