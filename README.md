# Selenium Java Automation Framework

A clean Selenium UI automation framework built with Java 17, Maven, TestNG, Page Object Model, WebDriverManager, ExtentReports, Log4j2, and JSON test data.

This project is intentionally beginner-friendly while keeping production-minded patterns: reusable base classes, explicit waits, ThreadLocal WebDriver management for parallel tests, external configuration, external test data, reporting, screenshots, and logging.

## Tech Stack

- Java 17 or newer
- Maven
- Selenium WebDriver
- TestNG
- WebDriverManager
- ExtentReports
- Log4j2
- Jackson Databind for JSON test data
- Apache Commons IO for screenshot file handling

## Folder Structure

```text
src
|-- main
|   `-- java
|       `-- com.automation
|           |-- base
|           |   |-- BasePage.java
|           |   `-- BaseTest.java
|           |-- config
|           |   `-- ConfigReader.java
|           |-- constants
|           |   `-- FrameworkConstants.java
|           |-- drivers
|           |   `-- DriverFactory.java
|           |-- listeners
|           |   `-- TestListener.java
|           |-- pages
|           |   |-- LoginPage.java
|           |   `-- DashboardPage.java
|           `-- utils
|               |-- DataProviderUtil.java
|               |-- ExtentReportManager.java
|               |-- JavaScriptUtil.java
|               |-- ScreenshotUtil.java
|               `-- WaitUtil.java
`-- test
    |-- java
    |   `-- com.automation.tests
    |       |-- DashboardTest.java
    |       `-- LoginTest.java
    `-- resources
        |-- config.properties
        |-- log4j2.xml
        |-- testng.xml
        `-- testdata
            `-- login-data.json
```

## Setup

Install:

1. Java 17 or newer
2. Maven
3. Chrome, Firefox, or Edge

Verify installation:

```bash
java -version
mvn -version
```

Maven downloads project dependencies automatically during the first test run.

## Configuration

Edit `src/test/resources/config.properties`:

```properties
browser=chrome
headless=false
baseUrl=https://example.com
timeout=10
remote=false
gridUrl=http://localhost:4444/wd/hub
demoMode=true
```

Supported browser values:

- `chrome`
- `firefox`
- `edge`
- `chrome-headless`

You can also run Chrome headless with:

```properties
browser=chrome
headless=true
```

For Selenium Grid:

```properties
remote=true
gridUrl=http://localhost:4444/wd/hub
```

## Demo Mode

The default `baseUrl` is `https://example.com`, and the page locators are placeholders. Because of that, `demoMode=true` skips browser startup and the sample UI assertions until you connect the framework to a real application.

Before using this framework against a real app:

1. Set `baseUrl` to the real application URL.
2. Update locators in `LoginPage.java` and `DashboardPage.java`.
3. Update `src/test/resources/testdata/login-data.json`.
4. Set `demoMode=false`.

## Run Tests

Run the full suite:

```bash
mvn clean test
```

Run the configured TestNG suite explicitly:

```bash
mvn test -DsuiteXmlFile=src/test/resources/testng.xml
```

The project is configured in `pom.xml` to run `src/test/resources/testng.xml` through Maven Surefire.

## Parallel Execution

Parallel execution is configured in `src/test/resources/testng.xml`:

```xml
<suite name="Selenium Automation Suite" parallel="methods" thread-count="2">
```

`DriverFactory` uses `ThreadLocal<WebDriver>`, so each parallel test method receives its own browser instance.

## Reports

ExtentReports are generated at:

```text
test-output/extent-report/AutomationReport.html
```

The report includes:

- Test name
- Test description
- Pass, fail, and skip status
- Error details
- Screenshots for failed tests

## Screenshots

Screenshots are captured automatically on test failure and saved to:

```text
test-output/screenshots
```

Filename format:

```text
testName_timestamp.png
```

## Logging

Log4j2 logs important framework actions:

- Browser launch
- URL opened
- Test started
- Test passed
- Test failed
- Screenshot captured
- Driver quit

Console and file logging are configured in:

```text
src/test/resources/log4j2.xml
```

The log file is written to:

```text
test-output/logs/automation.log
```

## Add a New Page Object

Create a new class under `src/main/java/com/automation/pages` and extend `BasePage`:

```java
public class ProfilePage extends BasePage {
    private final By profileHeader = By.cssSelector("[data-testid='profile-header']");

    public ProfilePage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return isDisplayed(profileHeader);
    }
}
```

Keep page classes focused on page actions and page verifications. Assertions should stay in test classes.

## Add a New Test

Create a new class under `src/test/java/com/automation/tests` and extend `BaseTest`:

```java
public class ProfileTest extends BaseTest {
    @Test
    public void profilePageShouldLoad() {
        skipIfDemoMode();
        ProfilePage profilePage = new ProfilePage(getDriver());
        Assert.assertTrue(profilePage.isLoaded(), "Profile page should be loaded.");
    }
}
```

Register the class in `src/test/resources/testng.xml`.

## Test Data

Login data is stored in:

```text
src/test/resources/testdata/login-data.json
```

`DataProviderUtil` reads the JSON file and exposes TestNG data providers:

- `validLoginData`
- `invalidLoginData`
- `emptyCredentialsData`

## Future Improvements

- Add environment-specific config files for QA, staging, and production.
- Add retry logic for known transient infrastructure failures.
- Add API helpers for test data setup.
- Add CI configuration for GitHub Actions, Jenkins, or GitLab CI.
- Add cross-browser matrix execution in Selenium Grid.
- Add Allure as an alternative reporting profile.
- Add encrypted secret handling for credentials.
