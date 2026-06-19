package com.automation.functions.HRIS;

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

    public void verifyEmployeeVisible() {
        ResolvedStepContext step = currentStep();
        requireXPath(step, "verifyEmployeeVisible");
        requireValue(step, "verifyEmployeeVisible", "Value");
        verifyTextContains();
    }
}
