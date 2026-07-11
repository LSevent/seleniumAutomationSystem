package com.automation.functions.DEMO;

import com.automation.base.BaseFunction;
import org.openqa.selenium.WebDriver;

public class SpecificFunction extends BaseFunction {

    public SpecificFunction(WebDriver driver) {
        super(driver);
    }

    public void captureScreenshotEvidence() {
        screenshot();
    }

    public void captureObjectScreenshotEvidence() {
        screenshotPartByObject();
    }

    public void captureScreenshotThenFail() {
        screenshot();
        throw new AssertionError("Intentional failure after screenshot evidence.");
    }
}
