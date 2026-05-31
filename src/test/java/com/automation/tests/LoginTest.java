package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.pages.DashboardPage;
import com.automation.pages.LoginPage;
import com.automation.utils.DataProviderUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test(dataProvider = "validLoginData", dataProviderClass = DataProviderUtil.class, description = "Verify valid user can log in.")
    public void validLoginTest(String username, String password) {
        skipIfDemoMode();

        LoginPage loginPage = new LoginPage(getDriver());
        DashboardPage dashboardPage = loginPage.login(username, password);

        Assert.assertTrue(dashboardPage.isDashboardLoaded(), "Dashboard should be loaded after valid login.");
    }

    @Test(dataProvider = "invalidLoginData", dataProviderClass = DataProviderUtil.class, description = "Verify invalid credentials show an error message.")
    public void invalidLoginTest(String username, String password, String expectedErrorMessage) {
        skipIfDemoMode();

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(username, password);

        Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed for invalid login.");
        Assert.assertTrue(
                loginPage.getErrorMessage().contains(expectedErrorMessage),
                "Error message should contain: " + expectedErrorMessage
        );
    }

    @Test(dataProvider = "emptyCredentialsData", dataProviderClass = DataProviderUtil.class, description = "Verify empty username or password validation.")
    public void emptyUsernamePasswordValidationTest(String username, String password, String expectedErrorMessage) {
        skipIfDemoMode();

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(username, password);

        Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Validation message should be displayed.");
        Assert.assertTrue(
                loginPage.getErrorMessage().toLowerCase().contains(expectedErrorMessage.toLowerCase()),
                "Validation message should contain: " + expectedErrorMessage
        );
    }
}
