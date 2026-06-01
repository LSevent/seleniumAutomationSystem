# Selenium Java Automation Framework

A clean Selenium UI automation framework built with Java 17, Maven, TestNG, Page Object Model, WebDriverManager, ExtentReports, Log4j2, and JSON test data.

This project is intentionally beginner-friendly while keeping production-minded patterns: reusable base classes, explicit waits, ThreadLocal WebDriver management for parallel tests, external configuration, external test data, reporting, screenshots, logging, and Excel-driven test design foundations.

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

## Excel Reader Foundation

Apache POI is used to read `.xlsx` files through `ExcelReader`.

`ExcelReader` can read workbook data by sheet name, row index, column index, and header name. Header row is row `0`, and header matching is case-insensitive with surrounding spaces ignored. Cell values are returned as `String`.

This is only the foundation for future Excel-driven execution. Scenario execution from Excel is not implemented yet.

## SCENARIOS Reader

The framework can read the `SCENARIOS` sheet from an Excel workbook. Only rows with `RUN` values of `Y`, `YES`, or `TRUE` are selected as active scenarios.

`NO` is the scenario data key and must be unique across the `SCENARIOS` sheet. Active scenarios require a nonblank `NO`, and future data sheets will use `NO` as the matching key.

`ACTION` must match an existing sheet name.

Scenario sheets use the headers `Testcase`, `Run`, `Function`, `Object`, `Value`, `Application`, and `Description`.

Scenario sheet execution is not implemented yet.

## Scenario Sheet Parser

The framework can now parse scenario sheets referenced by `SCENARIOS.ACTION`. Scenario sheets use a parent-child testcase format: a `Testcase` row starts a testcase block, and rows under it with `Function` are parsed as ordered step rows.

Execution order follows Excel row order. `Application` is required on active testcase rows; step rows inherit the parent `Application` unless they provide an override. `Description` is optional.

The framework does not execute Selenium steps from Excel yet.

## Data Reference Resolver

The framework can resolve data references in scenario sheet `Value` cells using dot notation:

```text
SHEET_NAME.COLUMN_NAME
```

Examples:

- `LOGIN_DATA.USERNAME`
- `LOGIN_DATA.PASSWORD`
- `BOOKING_DATA.ROOM_NAME`
- `BOOKING_DATA.EXPECTED_MESSAGE`

`SCENARIOS.NO` is used as the data key. Every data sheet must include a `NO` column, and the matching row is selected by the active scenario number.

If a `Value` cell is not a data reference, it is treated as literal text. Bracket notation is not supported.

## Object Repository Resolver

The framework can read the `OBJECT_REPOSITORY` sheet and resolve scenario step `Object` names into XPath values.

Object lookup uses `Application + Object`, so the same object name can exist in different applications. For example, `BRS.btnLogin` and `HRIS.btnLogin` can resolve to different repository rows.

Only XPath is supported for now. XPath values can contain one dynamic placeholder using `{COLUMN_NAME}`:

```text
//button[contains(text(),'{ROOM_NAME}')]
```

If the step `Value` is a data reference such as `BOOKING_DATA.ROOM_NAME`, the framework resolves it using `SCENARIOS.NO` first, then replaces `{ROOM_NAME}` with the resolved value. Literal step values can also fill a single placeholder.

KeywordEngine and Excel-driven Selenium execution are not implemented yet.

## Future Improvements

- Add environment-specific config files for QA, staging, and production.
- Add retry logic for known transient infrastructure failures.
- Add API helpers for test data setup.
- Add CI configuration for GitHub Actions, Jenkins, or GitLab CI.
- Add cross-browser matrix execution in Selenium Grid.
- Add Allure as an alternative reporting profile.
- Add encrypted secret handling for credentials.
