# Test Data Workbooks

`Final Excel Template.xlsx` is the project template for Excel-driven automation.

Normal workflow:

- Copy `Final Excel Template.xlsx` outside the repo before editing it for a real application.
- Update `excel.scenarioFilePath` in `src/test/resources/excelConfig.properties`, or override it with `-Dexcel.scenarioFilePath`.
- Update `CONFIG.BASE_URL` before executing against a real application.
- Keep all headers unchanged.
- Set `Run` on scenario rows and testcase parent rows. On individual step rows,
  blank/`Yes` runs the step and `No` reports it as skipped. Do not disable flow
  directive rows; use conditions to control flow blocks.
- Do not add DATA_ROW.
- Use `SHEET_NAME.COLUMN_NAME` for data references.
- Dot notation resolves data only when the named sheet exists. Values such as
  `john.doe`, `document.pdf`, and `123.45` otherwise remain literal text.
- Simple formula-header references such as `=LOGIN_DATA!$B$1` are also supported when the formula points to a data-sheet header cell.
- Put screenshot evidence labels in `Description`; screenshot keywords do not
  use `Value` as the label.
- Use the `Create New Booking` sheet as the sample for loop + condition flow:
  `forEachDataRow`, `ifEquals`, `ifDisplayed`, `elseIfEquals`,
  `elseIfDisplayed`, `else`, `endIf`, and `endForEachDataRow`.
- Put the actual value/data reference in `Value` and the expected comparison in
  `Description` for `ifEquals` and `elseIfEquals`. The older
  `ACTUAL = EXPECTED` value form is still accepted.
- Close nested flow blocks from the inside out: an inner `endIf` must appear
  before the outer `endIf` or `endForEachDataRow`.
- The configured Excel runner uses `explicitTime` for normal browser actions
  and the shorter `conditionTime` for `ifDisplayed` checks.
- Each run creates its own timestamped report folder. Evidence opens inside the
  offline HTML report with previous/next navigation.

`Template Testing.xlsx` is the stable workbook used by the automated tests in this repository.
