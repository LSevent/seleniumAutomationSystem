package com.automation.functions.RESOLVERTEST;

import com.automation.base.BaseFunction;
import com.automation.context.StepContextHolder;
import com.automation.models.ResolvedStepContext;
import org.openqa.selenium.WebDriver;

public class SpecificFunction extends BaseFunction {

    private static String invocation = "";
    private static String receivedXpath = "";
    private static String receivedValue = "";
    private static ResolvedStepContext receivedContext;

    public SpecificFunction(WebDriver driver) {
        super(driver);
    }

    public void preferNoArg() {
        invocation = "no-arg";
        receivedContext = StepContextHolder.get();
    }

    public void preferNoArg(String xpath) {
        invocation = "parameter:" + xpath;
    }

    @Override
    public String toString() {
        invocation = "specific-toString";
        return invocation;
    }

    public void legacyWithContext(String xpath, String value, ResolvedStepContext context) {
        invocation = "context-fallback";
        receivedXpath = xpath;
        receivedValue = value;
        receivedContext = context;
    }

    public static void reset() {
        invocation = "";
        receivedXpath = "";
        receivedValue = "";
        receivedContext = null;
    }

    public static String getInvocation() {
        return invocation;
    }

    public static String getReceivedXpath() {
        return receivedXpath;
    }

    public static String getReceivedValue() {
        return receivedValue;
    }

    public static ResolvedStepContext getReceivedContext() {
        return receivedContext;
    }
}
