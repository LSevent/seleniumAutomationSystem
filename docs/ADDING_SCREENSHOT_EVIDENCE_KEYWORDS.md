# How to Add Screenshot / Evidence Keywords

This framework treats screenshot and evidence commands as engine-level keywords.
They should not be added to `BaseFunction` or `SpecificFunction`.

The reason is simple: screenshot keywords need report configuration, screenshot
folder paths, evidence links, scenario/testcase/row context, and consistent
failure handling. That belongs near `KeywordEngine`, `ScreenshotKeywordHandler`,
`ScreenshotService`, and the report layer.

## Current screenshot keywords

| Keyword | Object | Value | Purpose |
| --- | --- | --- | --- |
| `screenshot` | Optional | Optional | Captures the current browser viewport as manual evidence. |
| `screenshotPartByObject` | Required | Optional | Captures the resolved object as one or more image parts. |

`Value` is normally used as the evidence label. If it is blank, the keyword can
fall back to a safe default such as the object name.

## Where screenshot keyword code belongs

Use this split:

```text
KeywordEngine
  Owns keyword lifecycle:
  - set StepContextHolder
  - log start/completion/failure/skip
  - clear StepContextHolder
  - delegate screenshot/evidence keywords

ScreenshotKeywordHandler
  Owns screenshot/evidence keyword decisions:
  - supports("screenshot")
  - supports("screenshotPartByObject")
  - build ExecutionResult
  - decide evidence label
  - handle skip when manual screenshots are disabled

ScreenshotService
  Owns screenshot mechanics:
  - take browser screenshot
  - take element screenshot
  - split/crop long element screenshots
  - save files

ExcelExecutionReporter
  Owns report rendering:
  - turn evidence paths into links
  - add screenshots to Evidence Gallery
  - preserve report layout
```

## Steps to add a new evidence keyword

Example new keyword:

```text
screenshotFullPage
```

### 1. Add keyword detection in `ScreenshotKeywordHandler`

Add the keyword to `supports(...)`.

```java
public boolean supports(String keywordName) {
    return isManualScreenshotKeyword(keywordName)
            || isScreenshotPartByObjectKeyword(keywordName)
            || isScreenshotFullPageKeyword(keywordName);
}
```

Then route it inside `execute(...)`.

```java
if (isScreenshotFullPageKeyword(step.getKeyword())) {
    return executeScreenshotFullPage(step);
}
```

Add a small matcher:

```java
private boolean isScreenshotFullPageKeyword(String keywordName) {
    return keywordName != null && "screenshotFullPage".equalsIgnoreCase(keywordName.trim());
}
```

### 2. Implement the handler method

The handler should return an `ExecutionResult`.

Use this pattern:

```java
private ExecutionResult executeScreenshotFullPage(ResolvedStepContext step) {
    String executedBy = executedBy(step);
    if (!reportConfig.isManualScreenshotEnabled()) {
        return skipped(step, executedBy);
    }

    try {
        String label = isBlank(step.getResolvedValue())
                ? "FullPageScreenshot"
                : step.getResolvedValue();

        String screenshotPath = screenshotService.captureFullPage(
                driver,
                screenshotBaseName(step, label)
        );

        String evidence = screenshotPath == null
                ? "Screenshot not available: driver does not support full-page screenshots."
                : screenshotPath;

        return ExecutionResult.success(
                step,
                executedBy,
                "REPORT",
                evidence,
                "Full-page screenshot captured."
        );
    } catch (RuntimeException exception) {
        return ExecutionResult.failure(
                step,
                executedBy,
                "REPORT",
                failureMessage(
                        "Failed to capture full-page screenshot for step row " + step.getExcelRow() + ".",
                        step,
                        exception
                )
        );
    }
}
```

Keep the `executionSource` as:

```text
REPORT
```

That keeps the report’s `Executed By` / source behavior consistent.

### 3. Add screenshot mechanics in `ScreenshotService`

If the keyword needs a new capture technique, add it to `ScreenshotService`.

Examples:

```java
public String captureFullPage(WebDriver driver, String screenshotName) {
    // save screenshot and return absolute path
}
```

or, for multiple images:

```java
public List<String> captureSomethingInParts(...) {
    // save each part and return absolute paths
}
```

Return saved PNG paths. Do not build report HTML here.

### 4. Return evidence in report-friendly format

For one screenshot:

```java
evidence = screenshotPath;
```

For multiple screenshots:

```java
evidence = String.join(System.lineSeparator(), screenshotPaths);
```

`ExcelExecutionReporter` already understands multiple `.png` evidence paths
separated by new lines. It renders them as separate links and gallery images.

### 5. Update pre-run validation

Update `PreRunValidator` so the keyword is known before runtime.

For a keyword that does not need an object:

```java
case "screenshotfullscreen" -> {
    // no Object/XPath/Value requirement
}
```

For a keyword that needs an object:

```java
case "screenshotpartbyobject" -> requireObjectAndXPath(step, keyword, errors);
```

Also make sure screenshot/evidence keywords are treated as known engine-level
keywords, not normal `BaseFunction` methods.

### 6. Update tests

Add focused tests:

- `KeywordEngineResolvedContextTest`
  - keyword is handled by the screenshot handler path
  - `StepContextHolder` is available during capture
  - result source is `REPORT`
  - evidence path is returned

- `PreRunValidatorTest`
  - required Object/XPath/Value rules are enforced
  - keyword is not reported as unknown

- `ExcelExecutionReportTest`
  - evidence appears as links
  - evidence gallery contains screenshots
  - multiple screenshot paths render as multiple items when applicable

### 7. Update documentation

Update the supported keyword table in `README.md`.

At minimum document:

- keyword name
- whether `Object` is required
- whether `Value` is required
- what evidence is created
- what `Value` means as a label

## Design rules

Keep these rules unless the framework architecture changes later:

- Do not add screenshot/evidence keywords to `BaseFunction`.
- Do not add screenshot/evidence keywords to `SpecificFunction`.
- Do not build report HTML inside `ScreenshotService`.
- Do not write screenshot files directly inside `KeywordEngine`.
- Use `ScreenshotKeywordHandler` for evidence keyword decisions.
- Use `ScreenshotService` for image capture and saving.
- Use `ExecutionResult.evidence` to pass file paths to the report.
- Keep screenshot paths as PNG files.
- Use one path for one screenshot.
- Use newline-separated paths for multiple screenshots.

## User-facing Excel example

Manual screenshot:

```text
Keyword     | Object | Value
screenshot  |        | After login
```

Object screenshot:

```text
Keyword                 | Object       | Value
screenshotPartByObject  | pnlBooking   | Booking panel
```

The report will show the screenshot evidence in the `Evidence` column and in
the `Evidence Gallery`.
