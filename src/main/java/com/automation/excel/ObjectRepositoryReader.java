package com.automation.excel;

import com.automation.exceptions.FrameworkException;
import com.automation.models.DataReference;
import com.automation.models.ResolvedObject;
import com.automation.models.Scenario;
import com.automation.models.TestObject;
import com.automation.models.TestStep;
import com.automation.utils.XPathResolver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ObjectRepositoryReader {

    private static final String OBJECT_REPOSITORY_SHEET = "OBJECT_REPOSITORY";
    private static final String APPLICATION_COLUMN = "Application";
    private static final String OBJECT_COLUMN = "Object";
    private static final String XPATH_COLUMN = "XPath";
    private static final String DESCRIPTION_COLUMN = "Description";

    private final ExcelReader excelReader;
    private final DataReader dataReader;
    private final XPathResolver xpathResolver;

    public ObjectRepositoryReader(ExcelReader excelReader, DataReader dataReader) {
        if (excelReader == null) {
            throw new IllegalArgumentException("ExcelReader must not be null.");
        }
        if (dataReader == null) {
            throw new IllegalArgumentException("DataReader must not be null.");
        }
        this.excelReader = excelReader;
        this.dataReader = dataReader;
        this.xpathResolver = new XPathResolver();
    }

    public List<TestObject> getAllObjects() {
        validateRequiredHeaders();

        List<TestObject> objects = new ArrayList<>();
        int lastRowNumber = excelReader.getLastRowNumber(OBJECT_REPOSITORY_SHEET);
        for (int rowIndex = 1; rowIndex <= lastRowNumber; rowIndex++) {
            ObjectRow objectRow = readRow(rowIndex);
            if (!objectRow.isBlank()) {
                validateObjectRow(objectRow, toExcelRowNumber(rowIndex));
                objects.add(toTestObject(objectRow, toExcelRowNumber(rowIndex)));
            }
        }

        validateDuplicateObjects(objects);
        return objects;
    }

    public TestObject getObject(String application, String objectName) {
        String normalizedApplication = normalize(application);
        String normalizedObjectName = normalize(objectName);

        if (normalizedApplication.isBlank()) {
            throw new FrameworkException("Application is required to resolve object '" + safeValue(objectName) + "'.");
        }
        if (normalizedObjectName.isBlank()) {
            throw new FrameworkException("Object name is required to resolve object.");
        }

        validateRequiredHeaders();
        TestObject matchedObject = null;
        int lastRowNumber = excelReader.getLastRowNumber(OBJECT_REPOSITORY_SHEET);
        for (int rowIndex = 1; rowIndex <= lastRowNumber; rowIndex++) {
            ObjectRow objectRow = readRow(rowIndex);
            if (!normalize(objectRow.application()).equals(normalizedApplication)
                    || !normalize(objectRow.objectName()).equals(normalizedObjectName)) {
                continue;
            }

            int excelRowNumber = toExcelRowNumber(rowIndex);
            validateObjectRow(objectRow, excelRowNumber);
            if (matchedObject != null) {
                throw new FrameworkException("Duplicate object found in OBJECT_REPOSITORY. Application = "
                        + matchedObject.getApplication() + ", Object = " + matchedObject.getObjectName() + ".");
            }
            matchedObject = toTestObject(objectRow, excelRowNumber);
        }

        if (matchedObject == null) {
            throw new FrameworkException("Object not found in OBJECT_REPOSITORY. Application = "
                    + application.trim() + ", Object = " + objectName.trim() + ".");
        }
        return matchedObject;
    }

    public ResolvedObject resolveObject(TestStep step, Scenario scenario) {
        if (step == null) {
            throw new IllegalArgumentException("TestStep must not be null.");
        }
        if (step.getObject() == null || step.getObject().isBlank()) {
            return null;
        }
        if (step.getApplication() == null || step.getApplication().isBlank()) {
            throw new FrameworkException("Application is required to resolve object '" + step.getObject().trim() + "'.");
        }

        TestObject testObject = getObject(step.getApplication(), step.getObject());
        String rawValue = step.getValue() == null ? "" : step.getValue().trim();
        String resolvedValue = resolveStepValue(rawValue, scenario);

        return resolveObject(step, rawValue, resolvedValue, testObject);
    }

    public ResolvedObject resolveObject(TestStep step, String rawValue, String resolvedValue) {
        if (step == null) {
            throw new IllegalArgumentException("TestStep must not be null.");
        }
        if (step.getObject() == null || step.getObject().isBlank()) {
            return null;
        }
        if (step.getApplication() == null || step.getApplication().isBlank()) {
            throw new FrameworkException("Application is required to resolve object '" + step.getObject().trim() + "'.");
        }

        TestObject testObject = getObject(step.getApplication(), step.getObject());
        return resolveObject(step, rawValue, resolvedValue, testObject);
    }

    private ResolvedObject resolveObject(TestStep step, String rawValue, String resolvedValue, TestObject testObject) {
        String safeRawValue = rawValue == null ? "" : rawValue.trim();
        String safeResolvedValue = resolvedValue == null ? "" : resolvedValue.trim();
        String resolvedXPath = resolveXPath(testObject.getXPath(), testObject.getObjectName(), safeRawValue, safeResolvedValue);
        return new ResolvedObject(
                step.getObject(),
                step.getApplication(),
                testObject.getXPath(),
                resolvedXPath,
                safeRawValue,
                safeResolvedValue,
                testObject.getExcelRowNumber()
        );
    }

    public String resolveXPath(TestStep step, Scenario scenario) {
        ResolvedObject resolvedObject = resolveObject(step, scenario);
        return resolvedObject == null ? "" : resolvedObject.getResolvedXPath();
    }

    public void validateObjectRepository() {
        getAllObjects();
    }

    private void validateRequiredHeaders() {
        if (!excelReader.isSheetExists(OBJECT_REPOSITORY_SHEET)) {
            throw new FrameworkException("Required sheet not found: OBJECT_REPOSITORY.");
        }

        findRequiredColumnIndex(APPLICATION_COLUMN);
        findRequiredColumnIndex(OBJECT_COLUMN);
        findRequiredColumnIndex(XPATH_COLUMN);
    }

    private ObjectRow readRow(int rowIndex) {
        return new ObjectRow(
                readCell(rowIndex, APPLICATION_COLUMN, true),
                readCell(rowIndex, OBJECT_COLUMN, true),
                readCell(rowIndex, XPATH_COLUMN, true),
                readCell(rowIndex, DESCRIPTION_COLUMN, false)
        );
    }

    private String readCell(int rowIndex, String columnName, boolean requiredHeader) {
        int columnIndex = requiredHeader ? findRequiredColumnIndex(columnName) : findOptionalColumnIndex(columnName);
        if (columnIndex < 0) {
            return "";
        }

        try {
            if (columnIndex >= excelReader.getColumnCount(OBJECT_REPOSITORY_SHEET, rowIndex)) {
                return "";
            }
            return excelReader.getCellValue(OBJECT_REPOSITORY_SHEET, rowIndex, columnIndex).trim();
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Row not found: row " + rowIndex + " in sheet " + OBJECT_REPOSITORY_SHEET)) {
                return "";
            }
            throw exception;
        }
    }

    private int findRequiredColumnIndex(String columnName) {
        try {
            return excelReader.findColumnIndex(OBJECT_REPOSITORY_SHEET, columnName);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Header not found:")) {
                throw new FrameworkException("Header not found: " + columnName + " in sheet OBJECT_REPOSITORY.", exception);
            }
            throw exception;
        }
    }

    private int findOptionalColumnIndex(String columnName) {
        try {
            return excelReader.findColumnIndex(OBJECT_REPOSITORY_SHEET, columnName);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Header not found:")) {
                return -1;
            }
            throw exception;
        }
    }

    private TestObject toTestObject(ObjectRow objectRow, int excelRowNumber) {
        return new TestObject(
                objectRow.application(),
                objectRow.objectName(),
                objectRow.xpath(),
                objectRow.description(),
                excelRowNumber
        );
    }

    private void validateObjectRow(ObjectRow objectRow, int excelRowNumber) {
        if (objectRow.application().isBlank()) {
            throw new FrameworkException("Application is required in OBJECT_REPOSITORY row " + excelRowNumber + ".");
        }
        if (objectRow.objectName().isBlank()) {
            throw new FrameworkException("Object is required in OBJECT_REPOSITORY row " + excelRowNumber + ".");
        }
        if (objectRow.xpath().isBlank()) {
            throw new FrameworkException("XPath is required in OBJECT_REPOSITORY row " + excelRowNumber + ".");
        }
    }

    private void validateDuplicateObjects(List<TestObject> objects) {
        Map<String, TestObject> objectByKey = new LinkedHashMap<>();
        for (TestObject testObject : objects) {
            String key = normalize(testObject.getApplication()) + "::" + normalize(testObject.getObjectName());
            TestObject existingObject = objectByKey.putIfAbsent(key, testObject);
            if (existingObject != null) {
                throw new FrameworkException("Duplicate object found in OBJECT_REPOSITORY. Application = "
                        + existingObject.getApplication() + ", Object = " + existingObject.getObjectName() + ".");
            }
        }
    }

    private String resolveStepValue(String rawValue, Scenario scenario) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        return dataReader.isDataReference(rawValue)
                ? dataReader.resolveValue(rawValue, scenario)
                : rawValue;
    }

    private String resolveXPath(String rawXPath, String objectName, String rawValue, String resolvedValue) {
        List<String> placeholders = xpathResolver.extractPlaceholders(rawXPath);
        if (placeholders.isEmpty()) {
            return rawXPath;
        }
        if (placeholders.size() > 1) {
            throw new FrameworkException("Multiple XPath placeholders are not supported in Phase 11 for object " + objectName + ".");
        }

        String placeholderName = placeholders.get(0);
        String placeholder = "{" + placeholderName + "}";
        if (placeholderName.isBlank()) {
            throw new FrameworkException("XPath placeholder cannot be blank for object " + objectName + ".");
        }
        if (rawValue == null || rawValue.isBlank()) {
            throw new FrameworkException("XPath placeholder " + placeholder + " requires a value for object " + objectName + ".");
        }

        if (dataReader.isDataReference(rawValue)) {
            DataReference dataReference = dataReader.parseReference(rawValue);
            if (!placeholderName.equalsIgnoreCase(dataReference.getColumnName())) {
                throw new FrameworkException("XPath placeholder " + placeholder + " does not match data column '"
                        + dataReference.getColumnName() + "' for object " + objectName + ".");
            }
        }
        if (resolvedValue == null || resolvedValue.isBlank()) {
            throw new FrameworkException("XPath placeholder " + placeholder + " could not be resolved for object " + objectName + ".");
        }

        String resolvedXPath = xpathResolver.replacePlaceholder(rawXPath, placeholderName, resolvedValue);
        if (xpathResolver.hasPlaceholder(resolvedXPath)) {
            throw new FrameworkException("XPath placeholder " + placeholder + " could not be resolved for object " + objectName + ".");
        }
        return resolvedXPath;
    }

    private static int toExcelRowNumber(int rowIndex) {
        return rowIndex + 1;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private record ObjectRow(String application, String objectName, String xpath, String description) {
        private boolean isBlank() {
            return application.isBlank()
                    && objectName.isBlank()
                    && xpath.isBlank()
                    && description.isBlank();
        }
    }
}
