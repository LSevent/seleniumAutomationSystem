# Test Data Workbooks

`Final Excel Template.xlsx` is the project template for Excel-driven automation.

Normal workflow:

- Copy `Final Excel Template.xlsx` outside the repo before editing it for a real application.
- Update `excel.scenarioFilePath` in `src/test/resources/excelConfig.properties`, or override it with `-Dexcel.scenarioFilePath`.
- Update `CONFIG.BASE_URL` before executing against a real application.
- Keep all headers unchanged.
- Set `Run` on scenario rows and testcase parent rows. Leave `Run` blank on
  individual step rows; step-level overrides are not currently supported.
- Do not add DATA_ROW.
- Use `SHEET_NAME.COLUMN_NAME` for data references.
- Put dotted literals such as `john.doe` in a data-sheet cell and reference the
  column, because dot notation is interpreted as a data reference.
- Simple formula-header references such as `=LOGIN_DATA!$B$1` are also supported when the formula points to a data-sheet header cell.
- Put screenshot evidence labels in `Description`; screenshot keywords do not
  use `Value` as the label.
- Use the `Create New Booking` sheet as the sample for loop + condition flow:
  `forEachDataRow`, `ifEquals`, `elseIfEquals`, `else`, `endIf`, and `endForEachDataRow`.

`Template Testing.xlsx` is the stable workbook used by the automated tests in this repository.
