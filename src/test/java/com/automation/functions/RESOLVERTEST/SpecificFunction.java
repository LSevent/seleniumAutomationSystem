package com.automation.functions.RESOLVERTEST;

import com.automation.base.BaseFunction;
import com.automation.context.StepContextHolder;
import com.automation.models.ResolvedStepContext;
import org.openqa.selenium.WebDriver;

public class SpecificFunction extends BaseFunction {

    private static String invocation = "";
    private static ResolvedStepContext receivedContext;

    public SpecificFunction(WebDriver driver) {
        super(driver);
    }

    public void preferNoArg() {
        invocation = "no-arg";
        receivedContext = StepContextHolder.get();
    }

    public static void reset() {
        invocation = "";
        receivedContext = null;
    }

    public static String getInvocation() {
        return invocation;
    }

    public static ResolvedStepContext getReceivedContext() {
        return receivedContext;
    }
}
