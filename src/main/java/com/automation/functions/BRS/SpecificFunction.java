package com.automation.functions.BRS;

import com.automation.base.BaseFunction;
import org.openqa.selenium.WebDriver;

public class SpecificFunction extends BaseFunction {

    public SpecificFunction(WebDriver driver) {
        super(driver);
    }
    
    public void debug() {
        System.out.println("Debugging SpecificFunction");
    }

    public void clickMultiValue() {
        String[] arrValues = value().split(";");

        for (String strValue : arrValues) {
            String targetXPath = xpath().replace("#", strValue.trim());
            try {
                waitForClickableElement(targetXPath, "click").click();
            } catch (Exception exception) {
                throw new RuntimeException("Failed to click element with value: " + strValue, exception);
            }
        }
    }
}
