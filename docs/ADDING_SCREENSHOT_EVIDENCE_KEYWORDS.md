# How to Add Screenshot and Evidence Keywords

Screenshot commands are normal reusable Excel keywords. Their public entry
points live in `BaseFunction`, while screenshot mechanics stay in
`ScreenshotService`.

This gives a custom keyword the simple usage expected by framework users:

```java
public void captureBookingEvidence() {
    screenshot();
}
```

`KeywordEngine` starts the screenshot and evidence context before invoking the
keyword, then collects the generated evidence paths for the report. A keyword
developer does not need to build `ExecutionResult` or report HTML.

## Current screenshot keywords

| Keyword | Object required | Value required | Purpose |
| --- | --- | --- | --- |
| `screenshot` | No | No | Captures the current browser viewport. |
| `screenshotPartByObject` | Yes | No | Scrolls through the resolved object or its page area and captures multiple viewport screenshots when needed. |
| `screenshotFullPart` | No | No | Scrolls the full browser page through `document.scrollingElement` and captures multiple viewport screenshots when needed. |

Use the scenario row's `Description` as the evidence label. Screenshot
keywords do not use `Value` as their label. For
`screenshotPartByObject`, a blank description falls back to the object name.

## Current responsibility split

```text
BaseFunction
  Public no-argument Excel keywords:
  - screenshot()
  - screenshotPartByObject()
  - screenshotFullPart()

KeywordSupport
  Shared protected support:
  - access the current resolved step
  - access the current ScreenshotService
  - register one or more evidence paths

ScreenshotService
  Screenshot mechanics:
  - save a browser screenshot
  - scroll an object and capture its visible parts
  - scroll the full page and capture its visible parts
  - return saved PNG paths

KeywordEngine
  Per-step lifecycle:
  - set StepContextHolder and screenshot/evidence context
  - invoke the resolved keyword
  - attach collected evidence to ExecutionResult
  - clear every context in finally

ExcelExecutionReporter
  Report presentation:
  - render evidence links
  - render the Evidence Gallery
  - create failure screenshots when enabled
```

## Reuse an existing screenshot keyword in SpecificFunction

Application-specific keywords extend `BaseFunction`, so they can compose the
existing screenshot commands directly:

```java
public void captureApprovedBooking() {
    click();
    screenshot();
}
```

For object evidence:

```java
public void captureLongBookingPanel() {
    screenshotPartByObject();
}
```

For full-page evidence:

```java
public void captureCompleteBookingPage() {
    screenshotFullPart();
}
```

The active Excel row still supplies the resolved object, XPath, description,
scenario, testcase, and row context. Every screenshot produced during the
custom keyword is attached to that step's report evidence.

## Add a new reusable screenshot keyword

Use this process only when the existing three screenshot commands do not cover
the required capture behavior.

### 1. Add the capture mechanic to ScreenshotService

Keep browser scrolling and file creation in `ScreenshotService`. Return the
saved path for one image or a list of paths for multiple images.

Example shape:

```java
public List<String> captureModalInParts(
        WebDriver driver,
        WebElement modal,
        ResolvedStepContext step,
        String label
) {
    // Scroll the modal, call the normal screenshot saver for each part,
    // and return the saved PNG paths.
}
```

Do not create report HTML in this service.

### 2. Add one protected evidence bridge when needed

If the new service method returns evidence paths, add one focused protected
helper in `KeywordSupport`. It should:

1. read the current step and screenshot service;
2. call the new `ScreenshotService` method;
3. register the returned paths through `EvidenceContextHolder`;
4. return the paths only when custom Java composition needs them.

Avoid chains of several keyword-specific overloads. Keep one simple default
path and, only when useful, one advanced overload for custom composition.

### 3. Add the public Excel keyword to BaseFunction

For a command reusable by every application, add a public no-argument method:

```java
public void screenshotModalInParts() {
    captureModalInParts();
}
```

Keep the public method small. It represents the command that Excel users see.

If the behavior only makes sense for one application, put the public method in
that application's `SpecificFunction` instead.

### 4. Update pre-run validation

Document and enforce the minimum required Excel fields:

- no target element: no Object/XPath requirement;
- target element: require Object and resolved XPath;
- business input: require Value only when the keyword actually consumes it.

Screenshot labels belong in `Description`, so do not require `Value` only for
labeling.

### 5. Add focused tests

At minimum verify:

- the new public keyword is a no-argument method;
- the resolved XPath is used when an object is required;
- one or multiple screenshot paths are registered as evidence;
- evidence appears in the report and Evidence Gallery;
- screenshot-disabled behavior is clear;
- scroll loops stop when the position no longer changes;
- scroll position is restored when required;
- `StepContextHolder`, screenshot context, and evidence context are cleared.

### 6. Update user documentation

Add the command to the supported keyword table with:

- keyword name;
- Object requirement;
- Value requirement;
- Description/label behavior;
- expected screenshot behavior.

## Design rules

- Keep public Excel-facing screenshot keywords in `BaseFunction` when they are
  reusable across applications.
- Keep application-specific screenshot workflows in `SpecificFunction`.
- Keep scrolling and file-saving mechanics in `ScreenshotService`.
- Keep report HTML in `ExcelExecutionReporter`.
- Use `Description`, not `Value`, for evidence labels.
- Return PNG paths from screenshot mechanics.
- Register every saved path as step evidence.
- Keep the maximum-part guard so a broken or continuously growing page cannot
  create an endless screenshot loop.
- Do not access report internals from a keyword method.

## Excel examples

Normal screenshot:

```text
Keyword    | Object | Value | Description
screenshot |        |       | After login
```

Object screenshot in parts:

```text
Keyword                 | Object     | Value | Description
screenshotPartByObject  | pnlBooking |       | Booking panel
```

Full-page screenshot in parts:

```text
Keyword            | Object | Value | Description
screenshotFullPart |        |       | Complete booking page
```

The report displays each saved image in the `Evidence` column and the Evidence
Gallery.
