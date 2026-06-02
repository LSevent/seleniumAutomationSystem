package com.automation.tests;

import com.automation.reports.SensitiveDataMasker;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SensitiveDataMaskerTest {

    @Test
    public void passwordReferencesShouldMaskResolvedValues() {
        SensitiveDataMasker masker = new SensitiveDataMasker();

        String maskedValue = masker.maskIfNeeded(
                "brs123",
                false,
                "LOGIN_DATA.PASSWORD",
                "txtPassword",
                "//input[@id='password']",
                "Input password",
                "input"
        );

        Assert.assertEquals(maskedValue, SensitiveDataMasker.MASK);
    }

    @Test
    public void passwordObjectNamesShouldMaskLiteralValues() {
        SensitiveDataMasker masker = new SensitiveDataMasker();

        Assert.assertEquals(
                masker.maskRawValueIfNeeded("MySecret123", false, "txtPassword", "", "Input password", "input"),
                SensitiveDataMasker.MASK
        );
    }

    @Test
    public void nonSensitiveValuesShouldNotBeMasked() {
        SensitiveDataMasker masker = new SensitiveDataMasker();

        Assert.assertEquals(
                masker.maskIfNeeded("Weekly Meeting", false, "BOOKING_DATA.BOOKING_TITLE", "txtBookingTitle", "", "Input title", "input"),
                "Weekly Meeting"
        );
    }

    @Test
    public void showSensitiveDataShouldReturnOriginalValue() {
        SensitiveDataMasker masker = new SensitiveDataMasker();

        Assert.assertEquals(
                masker.maskIfNeeded("brs123", true, "LOGIN_DATA.PASSWORD", "txtPassword", "//input[@id='password']", "Input password", "input"),
                "brs123"
        );
    }
}
