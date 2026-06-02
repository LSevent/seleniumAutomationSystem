# Test Data Workbooks

`Final Excel Template.xlsx` is the user-facing example template for Excel-driven automation.

Recommended use:

- Copy `Final Excel Template.xlsx` outside the repo before editing it for a real project.
- Update `excel.scenarioFilePath` in `src/test/resources/excelConfig.properties`, or override it with `-Dexcel.scenarioFilePath`.
- Update `CONFIG.BASE_URL` before executing against a real application.
- Keep all headers unchanged.
- Do not add DATA_ROW.
- Use `SHEET_NAME.COLUMN_NAME` for data references.

`Template Testing.xlsx` is the stable workbook used by the automated tests in this repository.
