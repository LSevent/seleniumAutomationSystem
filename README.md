# Selenium Java Automation Framework

A Java 17 Selenium automation framework for normal TestNG UI tests and Excel-driven keyword execution.

The project is designed to be beginner-friendly while still using production-minded patterns: Page Object Model, `ThreadLocal` WebDriver management, explicit waits, external configuration, external test data, Excel scenario parsing, data resolution, object repository resolution, keyword dispatching, screenshots, logging, validation, and HTML reporting.

## Overview

The framework supports two testing styles:

- Standard TestNG tests that use page objects such as `LoginPage` and `DashboardPage`.
- Excel-driven tests that read scenarios, testcases, steps, test data, and object locators from an `.xlsx` workbook.

The Excel runner is the main automation design now. A scenario is selected from the `SCENARIOS` sheet, its action points to a scenario sheet, steps are parsed in Excel row order, values and XPath locators are resolved, then keywords are executed through application-specific or common Selenium functions.

## Tech Stack

- Java 17 or newer
- Maven
- Selenium WebDriver
- TestNG
- WebDriverManager
- ExtentReports
- Log4j2
- Apache POI
- Jackson Databind
- Apache Commons IO

## Project Structure

```text
src
|-- main
|   `-- java
|       `-- com.automation
|           |-- base
|           |-- config
|           |-- constants
|           |-- drivers
|           |-- engine
|           |-- excel
|           |-- exceptions
|           |-- functions
|           |   |-- BRS
|           |   |-- CRM
|           |   `-- HRIS
|           |-- listeners
|           |-- models
|           |-- pages
|           |-- reports
|           |-- services
|           `-- utils
`-- test
    |-- java
    |   `-- com.automation.tests
    `-- resources
        |-- config.properties
        |-- excelConfig.properties
        |-- log4j2.xml
        |-- testng.xml
        |-- test-pages
        `-- testdata
            |-- Final Excel Template.xlsx
            |-- Template Testing.xlsx
            |-- README.md
            `-- login-data.json
```

## Excel Execution Flow

1. `ExcelExecutionConfig` loads the Excel file path, report path, screenshot path, and report flags.
2. `ScenarioReader` reads `SCENARIOS` and selects active rows where `RUN` is `Y`, `YES`, or `TRUE`.
3. `StepReader` parses the scenario sheet named by `SCENARIOS.ACTION`.
4. Parent testcase rows define testcase name, run flag, application, and description.
5. Step rows inherit `Run` and `Application` from the latest parent testcase row unless the step overrides them.
6. `DataReader` resolves step `Value` cells that use `SHEET_NAME.COLUMN_NAME`.
7. `ObjectRepositoryReader` resolves step `Object` names into XPath values for the current application.
8. `KeywordEngine` executes the step. The special `screenshot` keyword is handled by the engine for reporting evidence.
9. `KeywordResolver` looks for the keyword in application-specific `SpecificFunction` first, then `BaseFunction`.
10. `ScenarioRunner` collects step results and sends them to `ExcelExecutionReporter`.

## Pre-run Validation

Before browser keyword execution starts, the framework builds a resolved execution plan for active scenarios and active testcases. The plan resolves data references, object repository references, raw and resolved XPath values, and dynamic XPath placeholders.

Pre-run validation checks data references, object and XPath availability, and simple keyword requirements such as the value required by `openUrl` and the XPath/value required by `input`. This catches Excel mistakes before Selenium steps begin. Inactive scenarios and inactive testcases are not included in the resolved plan and do not block execution.

## Step Execution Context

Public keyword methods are context-based, no-argument entry points. They read the current `ResolvedStepContext` from `StepContextHolder`, so common keywords such as `openUrl()`, `click()`, `input()`, `verifyDisplayed()`, and `verifyText()` use the resolved Excel step instead of receiving XPath or value arguments directly.

`PreRunValidator` validates required XPath and Value data before runtime. `BaseFunction` and `SpecificFunction` then read the already-validated resolved data through holder-backed helpers such as `xpath()` and `value()`, keeping runtime keyword methods focused on browser actions.

`ResolvedStepContext` is an immutable model for one resolved Excel step and should be created through `builder()`. Its long constructor is intentionally private to prevent parameter-order mistakes, while convenience accessors such as `xpath()` and `value()` keep consumers concise. During execution, `StepContextHolder` stores that single context in a `ThreadLocal`; `KeywordEngine` owns the set/clear lifecycle. Its `toString()` omits raw and resolved values so sensitive step data is not exposed accidentally.

Model `toString()` methods avoid exposing raw or resolved Excel values in logs and debug output. Reports still display values according to the existing report masking rules.

`ExecutionResult` and `TestStep` support builder-based construction. Builders are preferred for readability and to avoid parameter-order mistakes, while existing runtime behavior remains unchanged.

SpecificFunction keywords also use no-argument context-based execution for application-specific behavior. `BaseFunction` remains the home for common reusable keywords, while `KeywordResolver` checks `SpecificFunction` before `BaseFunction`.

The framework resolves an Excel keyword into a `ResolvedKeyword`; `KeywordExecutionResult` represents the result of invoking that resolved keyword method, and `KeywordSourceType` identifies whether the keyword came from `SpecificFunction` or `BaseFunction`.

`KeywordEngine` centrally logs keyword start, completion, skip, and failure events using the resolved step context, with sensitive values masked. Internal helper methods are limited to shared context access, validation, waiting, and reusable custom keyword support; they are not Excel-facing keyword entry points.

Excel execution now builds and validates a resolved execution plan before runtime startup. Runtime execution uses each `ResolvedStepContext` as its source of truth; `KeywordEngine` sets `StepContextHolder` for the step and clears it afterward, and report rows are populated from the same resolved step data.

## Configuration

Browser and normal UI-test configuration lives in:

```text
src/test/resources/config.properties
```

Important keys:

```properties
browser=chrome
headless=false
baseUrl=https://example.com
timeout=10
remote=false
gridUrl=http://localhost:4444/wd/hub
demoMode=true
report.showSensitiveData=false
report.screenshotOnFailure=true
report.manualScreenshotEnabled=true
```

Excel execution configuration lives in:

```text
src/test/resources/excelConfig.properties
```

Default values keep the test suite stable inside the repository:

```properties
excel.scenarioFilePath=src/test/resources/testdata/Template Testing.xlsx
report.outputDirectory=test-output/reports
report.fileName=ExcelAutomationReport.html
screenshot.outputDirectory=test-output/screenshots
```

`Final Excel Template.xlsx` is the user-facing sample workbook. The default config stays pointed at `Template Testing.xlsx` because that file is the stable automated-test workbook.

You can override Excel settings from Maven:

```bash
mvn test -Dexcel.scenarioFilePath="C:/Automation/scenarios/BookingRoomScenarios.xlsx" -Dreport.outputDirectory="C:/Automation/reports"
```

## Excel Template Format

The final example workbook is:

```text
src/test/resources/testdata/Final Excel Template.xlsx
```

It contains these sheets:

- `SCENARIOS`
- `CONFIG`
- `LOGIN_DATA`
- `BOOKING_DATA`
- `Local Keyword Test`
- `Create New Booking`
- `Cancel Booking`
- `OBJECT_REPOSITORY`

`SCENARIOS` must use this header:

```text
NO | RUN | ACTION | SCENARIOS
```

All Excel readers resolve columns by header name. Header matching trims surrounding spaces and ignores case, so column order does not matter as long as required headers are present.

Example:

```text
1 | Y | Local Keyword Test | Local keyword execution test
2 | N | Create New Booking | Create booking room example
3 | N | Cancel Booking | Cancel booking example
```

Scenario sheets must use this header:

```text
Testcase | Run | Keyword | Object | Value | Application | Description
```

`Keyword` is the preferred step header and is used by `Final Excel Template.xlsx`. Older workbooks may continue to use `Function` as a legacy alias. Internally this value is named `keyword`; `KeywordEngine` and `KeywordResolver` use it to select and execute the matching keyword method.

Rules:

- Execution follows Excel row order.
- A row with `Testcase` filled is a testcase parent row.
- Rows below a parent row are step rows.
- Parent rows require `Application`.
- Step rows inherit `Run` and `Application` from the latest parent row.
- Step row `Application` can override the parent only when needed.
- `Description` is optional.

Data sheets use `NO` as the matching key. The active scenario `NO` selects the matching row from each data sheet.

The current final template uses this `OBJECT_REPOSITORY` header order:

```text
Object | XPath | Application | Description
```

The framework resolves object repository columns by header name, so `Application`, `Object`, and `XPath` can be read even when their order changes. `Description` is optional.

## Data References

Step values can reference data sheets with dot notation:

```text
SHEET_NAME.COLUMN_NAME
```

Examples:

- `CONFIG.BASE_URL`
- `LOGIN_DATA.USERNAME`
- `LOGIN_DATA.PASSWORD`
- `BOOKING_DATA.ROOM_NAME`
- `BOOKING_DATA.EXPECTED_MESSAGE`

If a step `Value` does not match a data reference, the framework treats it as literal text.

## Dynamic XPath

The object repository supports a single dynamic placeholder in an XPath:

```text
//button[contains(text(),'{ROOM_NAME}')]
```

When a step uses:

```text
Object = btnRoomByName
Value = BOOKING_DATA.ROOM_NAME
```

the framework resolves `BOOKING_DATA.ROOM_NAME` for the current scenario `NO`, then replaces `{ROOM_NAME}` in the XPath.

Another example:

```text
//button[@data-booking='{BOOKING_ID}']
```

## Keywords

Common Selenium keywords live in `BaseFunction`.

Implemented common keywords include:

- `openUrl`
- `click`
- `input`
- `clear`
- `getText`
- `verifyDisplayed`
- `verifyText`
- `verifyTextContains`
- `verifyUrlContains`
- `verifyTitle`
- `verifyTitleContains`
- `waitVisible`
- `waitClickable`
- `scrollToElement`
- `safeClick`
- `pressEnter`
- `isDisplayed`
- `isNotDisplayed`

The `screenshot` keyword is special. It is handled by `KeywordEngine`, not by `BaseFunction`, because it needs scenario, testcase, step, report config, screenshot naming, and evidence-link context.

## BaseFunction vs SpecificFunction

`BaseFunction` contains reusable Selenium actions that are shared by all applications.

`SpecificFunction` contains application-specific behavior. Its keyword methods can use the same no-argument context style as `BaseFunction`: the current `ResolvedStepContext` is read from `StepContextHolder`, including the resolved XPath and value prepared by the execution plan.

Application-specific keywords live in:

```text
src/main/java/com/automation/functions/{APPLICATION}/SpecificFunction.java
```

Examples:

- `com.automation.functions.BRS.SpecificFunction`
- `com.automation.functions.HRIS.SpecificFunction`
- `com.automation.functions.CRM.SpecificFunction`

Application values from Excel are normalized to uppercase package segments. For example, `brs`, `Brs`, and `BRS` resolve to `BRS`.

Keyword lookup order:

1. No-argument method in `SpecificFunction` for the current `Application`
2. No-argument method in `BaseFunction`
3. Clear failure when the keyword is not found

If the same no-argument method exists in both `SpecificFunction` and `BaseFunction`, the application-specific method wins. Public parameter-based keyword fallback is no longer part of runtime resolution.

## Screenshots

Failure screenshots are controlled by:

```properties
report.screenshotOnFailure=true
```

Manual Excel screenshot steps are controlled by:

```properties
report.manualScreenshotEnabled=true
```

Manual screenshot step example:

```text
Keyword = screenshot
Value = After select room
```

Screenshots are saved under the configured screenshot directory. The default is:

```text
test-output/screenshots
```

Manual screenshots and failure screenshots are attached as evidence in the Excel HTML report.

## HTML Report

Excel execution generates a dedicated report at the configured report directory and file name. The default is:

```text
test-output/reports/ExcelAutomationReport.html
```

This report is separate from the generic TestNG method-level ExtentReport.

The Excel report hierarchy is:

```text
Scenario
  -> Testcase
      -> Steps
```

Every testcase uses the same step columns:

```text
Step | Excel Row | Description | Keyword | Object | Application | Raw Value | Resolved Value | Raw XPath | Resolved XPath | Executed By | Status | Evidence
```

Sensitive resolved values are masked by default:

```properties
report.showSensitiveData=false
```

For example, resolved password values display as:

```text
****
```

## Validations And Error Handling

The Excel flow fails early with clear validation messages.

Current validation coverage includes:

- `SCENARIOS` sheet exists.
- `SCENARIOS` contains `NO`, `RUN`, `ACTION`, and `SCENARIOS`.
- Active scenario `NO` is required and unique.
- Active scenario `ACTION` matches an existing sheet.
- Scenario sheets contain `Testcase`, `Run`, `Keyword`, `Object`, `Value`, `Application`, and `Description`. `Keyword` is the preferred step column; the legacy `Function` header remains supported for older workbooks.
- Active testcase parent rows require `Application`.
- Step rows inherit parent `Run` and `Application`.
- Data references use `SHEET_NAME.COLUMN_NAME`.
- Data sheets contain `NO`.
- Object repository rows are unique by `Application + Object`.
- XPath placeholder replacement supports one placeholder.
- Keyword lookup fails clearly when no matching method exists.
- Execution errors include scenario, testcase, Excel row, keyword, object, and application context where available.

Example validation messages:

```text
Scenario NO is required in sheet SCENARIOS row 2.
Scenario sheet not found: Create New Booking. Referenced by SCENARIOS row 2.
Object not found in OBJECT_REPOSITORY. Application = BRS, Object = btnMissing.
Keyword 'approveBooking' not found in SpecificFunction for application 'BRS' or BaseFunction.
```

## How To Run

Run the full suite:

```bash
mvn clean test
```

Run the configured TestNG suite explicitly:

```bash
mvn test -DsuiteXmlFile=src/test/resources/testng.xml
```

Run with a different Excel workbook:

```bash
mvn test -Dexcel.scenarioFilePath="C:/Automation/scenarios/BookingRoomScenarios.xlsx"
```

Run tests in parallel by editing:

```text
src/test/resources/testng.xml
```

The suite currently uses:

```xml
<suite name="Selenium Automation Suite" parallel="methods" thread-count="2">
```

## Current Limitations

- The final template is a sample. Real application URLs, objects, and data must be updated before production use.
- The default Excel config points to the stable test workbook, not the final user template.
- Excel execution stops on the first failed step in the current scenario flow.
- Retry logic is not implemented.
- Parallel Excel scenario execution is not implemented.
- Only XPath locators are supported in the object repository.
- Dynamic XPath replacement supports one placeholder per XPath.
- PDF export and Excel result export are not implemented.
- Secret management is not implemented.

## Recommended Next Improvements

- Add environment profiles for QA, staging, and production.
- Add retry rules for selected infrastructure failures.
- Add CI configuration for GitHub Actions, Jenkins, or GitLab CI.
- Add encrypted or external secret handling for credentials.
- Add more locator strategies beyond XPath.
- Add result export back into Excel.
- Add grouped scenario execution or tags.
- Add parallel Excel scenario execution after runner isolation is complete.
- Add richer application-specific keyword examples.
