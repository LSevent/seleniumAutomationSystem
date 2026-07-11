package com.automation.functions.HRIS;

import com.automation.base.BaseFunction;
import org.openqa.selenium.WebDriver;

public class SpecificFunction extends BaseFunction {

    public SpecificFunction(WebDriver driver) {
        super(driver);
    }

    public void verifyEmployeeVisible() {
        verifyTextContains();
    }
}
