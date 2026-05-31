package com.automation.pages;

import com.automation.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DashboardPage extends BasePage {

    // TODO: Replace these placeholder locators with locators from the target application.
    private final By pageHeader = By.cssSelector("h1, h2, [data-testid='dashboard-header']");
    private final By logoutButton = By.cssSelector("a[href*='logout'], button[data-testid='logout']");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public boolean isDashboardLoaded() {
        return isDisplayed(pageHeader) || isDisplayed(logoutButton);
    }

    public String getPageHeaderText() {
        return getText(pageHeader);
    }

    public LoginPage logout() {
        safeClick(logoutButton);
        return new LoginPage(driver);
    }
}
