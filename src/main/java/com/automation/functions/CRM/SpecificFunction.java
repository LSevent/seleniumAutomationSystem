package com.automation.functions.CRM;

import com.automation.base.BaseFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

public class SpecificFunction extends BaseFunction {

    private static final Logger LOGGER = LogManager.getLogger(SpecificFunction.class);

    public SpecificFunction(WebDriver driver) {
        super(driver);
    }

    public void waitForApplicationReady() {
        LOGGER.info("Executing CRM application readiness placeholder.");
    }
}
