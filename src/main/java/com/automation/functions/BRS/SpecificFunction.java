package com.automation.functions.BRS;

import com.automation.base.BaseFunction;
import org.openqa.selenium.WebDriver;

public class SpecificFunction extends BaseFunction {

    public SpecificFunction(WebDriver driver) {
        super(driver);
    }

    public void selectRoomByName() {
        safeClick();
    }

    public void verifyBookingCreated() {
        verifyTextContains();
    }
}
