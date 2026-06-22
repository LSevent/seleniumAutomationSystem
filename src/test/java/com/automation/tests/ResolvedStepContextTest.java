package com.automation.tests;

import com.automation.models.ResolvedStepContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ResolvedStepContextTest {

    @Test
    public void shouldExposeAllResolvedStepFields() {
        ResolvedStepContext context = ResolvedStepContext.builder()
                .scenarioNo("SC-001")
                .scenarioAction("BOOKING")
                .scenarioName("Create a booking")
                .sheetName("BOOKING")
                .testcaseName("Create booking successfully")
                .testcaseParentRow(3)
                .excelRow(5)
                .stepNumber(2)
                .function("input")
                .objectName("txtBookingTitle")
                .application("BRS")
                .description("Input booking title")
                .rawValue("BOOKING_DATA.TITLE")
                .resolvedValue("Weekly meeting")
                .rawXPath("//input[@data-id='${BOOKING_DATA.FIELD_ID}']")
                .resolvedXPath("//input[@data-id='booking-title']")
                .executedBy("com.automation.base.BaseFunction")
                .build();

        Assert.assertEquals(context.getScenarioNo(), "SC-001");
        Assert.assertEquals(context.getScenarioAction(), "BOOKING");
        Assert.assertEquals(context.getScenarioName(), "Create a booking");
        Assert.assertEquals(context.getSheetName(), "BOOKING");
        Assert.assertEquals(context.getTestcaseName(), "Create booking successfully");
        Assert.assertEquals(context.getTestcaseParentRow(), 3);
        Assert.assertEquals(context.getExcelRow(), 5);
        Assert.assertEquals(context.getStepNumber(), 2);
        Assert.assertEquals(context.getFunction(), "input");
        Assert.assertEquals(context.getObjectName(), "txtBookingTitle");
        Assert.assertEquals(context.getApplication(), "BRS");
        Assert.assertEquals(context.getDescription(), "Input booking title");
        Assert.assertEquals(context.getRawValue(), "BOOKING_DATA.TITLE");
        Assert.assertEquals(context.getResolvedValue(), "Weekly meeting");
        Assert.assertEquals(context.getRawXPath(), "//input[@data-id='${BOOKING_DATA.FIELD_ID}']");
        Assert.assertEquals(context.getResolvedXPath(), "//input[@data-id='booking-title']");
        Assert.assertEquals(context.getExecutedBy(), "com.automation.base.BaseFunction");
    }
}
