package com.automation.tests;

import com.automation.models.ResolvedStepContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ResolvedStepContextTest {

    @Test
    public void shouldExposeAllResolvedStepFields() {
        ResolvedStepContext context = new ResolvedStepContext(
                "SC-001",
                "BOOKING",
                "Create a booking",
                "BOOKING",
                "Create booking successfully",
                3,
                5,
                2,
                "input",
                "txtBookingTitle",
                "BRS",
                "Input booking title",
                "BOOKING_DATA.TITLE",
                "Weekly meeting",
                "//input[@data-id='${BOOKING_DATA.FIELD_ID}']",
                "//input[@data-id='booking-title']",
                "com.automation.base.BaseFunction"
        );

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
