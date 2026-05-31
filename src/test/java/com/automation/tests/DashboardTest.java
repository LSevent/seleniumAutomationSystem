package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.pages.DashboardPage;
import com.automation.pages.LoginPage;
import com.automation.utils.DataProviderUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DashboardTest extends BaseTest {

    @Test(dataProvider = "validLoginData", dataProviderClass = DataProviderUtil.class, description = "Verify dashboard content is visible after login.")
    public void dashboardVisibilityTest(String username, String password) {
        skipIfDemoMode();

        LoginPage loginPage = new LoginPage(getDriver());
        DashboardPage dashboardPage = loginPage.login(username, password);

        Assert.assertTrue(dashboardPage.isDashboardLoaded(), "Dashboard page should be visible.");
        Assert.assertFalse(dashboardPage.getPageHeaderText().isBlank(), "Dashboard header should not be blank.");
    }
}
