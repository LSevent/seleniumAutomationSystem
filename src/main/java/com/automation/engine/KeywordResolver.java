package com.automation.engine;

import com.automation.base.BaseFunction;
import com.automation.context.StepContextHolder;
import com.automation.exceptions.ErrorContext;
import com.automation.exceptions.FrameworkException;
import com.automation.models.KeywordExecutionResult;
import com.automation.models.KeywordSourceType;
import com.automation.models.ResolvedKeyword;
import com.automation.models.ResolvedStepContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public class KeywordResolver {

    private static final Logger LOGGER = LogManager.getLogger(KeywordResolver.class);
    private final WebDriver driver;
    private final BaseFunction baseFunction;
    private final KeywordCatalog keywordCatalog;

    public KeywordResolver(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver must not be null.");
        }
        this.driver = driver;
        this.baseFunction = new BaseFunction(driver);
        this.keywordCatalog = new KeywordCatalog();
    }

    public ResolvedKeyword resolve(String application, String keywordName) {
        return resolveInternal(
                application,
                keywordName,
                StepContextHolder.current().orElse(null)
        ).resolvedKeyword();
    }

    public WebDriver getDriver() {
        return driver;
    }

    public KeywordExecutionResult execute(String application, String keywordName) {
        ResolvedStepContext step = StepContextHolder.get();
        MethodResolution resolution = resolveInternal(application, keywordName, step);
        String keyword = keywordName.trim();

        try {
            resolution.method().invoke(resolution.target());
            String message = "Executed keyword '" + keyword + "' using "
                    + resolution.resolvedKeyword().getResolvedClassName() + ".";
            return new KeywordExecutionResult(
                    resolution.resolvedKeyword().getApplication(),
                    resolution.resolvedKeyword().getKeywordName(),
                    resolution.resolvedKeyword().getResolvedClassName(),
                    resolution.resolvedKeyword().getSourceType(),
                    true,
                    message
            );
        } catch (IllegalAccessException exception) {
            throw keywordExecutionFailure(keyword, resolution.resolvedKeyword(), exception, step);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw keywordExecutionFailure(keyword, resolution.resolvedKeyword(), cause, step);
        }
    }

    private MethodResolution resolveInternal(
            String application,
            String keywordName,
            ResolvedStepContext step
    ) {
        validateKeywordName(keywordName);
        String keyword = keywordName.trim();
        String normalizedApplication = normalizeApplication(application, keyword);

        LOGGER.debug("Resolving keyword '{}' for application '{}'.", keyword, normalizedApplication);

        KeywordCatalog.KeywordDefinition definition = keywordCatalog.discover(normalizedApplication, keyword)
                .orElseThrow(() -> new FrameworkException(withStepContext(
                        "Keyword '" + keyword + "' not found in SpecificFunction for application '"
                                + normalizedApplication + "' or BaseFunction.",
                        step
                )));

        Object target = definition.sourceType() == KeywordSourceType.SPECIFIC
                ? createSpecificFunction(definition.implementationClass(), normalizedApplication)
                : baseFunction;
        ResolvedKeyword resolvedKeyword = new ResolvedKeyword(
                normalizedApplication,
                keyword,
                definition.implementationClass().getName(),
                definition.sourceType(),
                definition.method().getName()
        );
        LOGGER.debug(
                "Selected {} for keyword '{}': {}",
                definition.sourceType() == KeywordSourceType.SPECIFIC ? "SpecificFunction" : "BaseFunction",
                keyword,
                definition.implementationClass().getName()
        );
        return new MethodResolution(resolvedKeyword, definition.method(), target);
    }

    private Object createSpecificFunction(Class<?> functionClass, String application) {
        try {
            Constructor<?> constructor = functionClass.getConstructor(WebDriver.class);
            return constructor.newInstance(driver);
        } catch (NoSuchMethodException exception) {
            throw new FrameworkException("SpecificFunction for application '" + application
                    + "' must have a constructor that accepts WebDriver.", exception);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new FrameworkException("Could not create SpecificFunction for application '" + application + "'.", exception);
        }
    }

    private RuntimeException keywordExecutionFailure(
            String keyword,
            ResolvedKeyword resolvedKeyword,
            Throwable cause,
            ResolvedStepContext step
    ) {
        String resolvedClass = simpleClassName(resolvedKeyword.getResolvedClassName());
        String message = "Keyword '" + keyword + "' failed in " + resolvedClass + ": "
                + conciseCauseMessage(cause);
        message = withStepContext(message, step);
        LOGGER.debug("Keyword invocation failed in {}.", resolvedClass, cause);
        if (cause instanceof AssertionError) {
            throw new AssertionError(message, cause);
        }
        return new FrameworkException(message, cause);
    }

    private String normalizeApplication(String application, String keyword) {
        if (isBlank(application)) {
            throw new FrameworkException("Application is required to resolve keyword '" + keyword + "'.");
        }
        return application.trim().toUpperCase(Locale.ROOT);
    }

    private void validateKeywordName(String keywordName) {
        if (isBlank(keywordName)) {
            throw new FrameworkException("Keyword name is required.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String simpleClassName(String className) {
        if (className == null || className.isBlank()) {
            return "";
        }
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private String conciseCauseMessage(Throwable cause) {
        if (cause == null) {
            return "Unknown keyword execution error.";
        }
        String message = cause.getMessage();
        if (isBlank(message)) {
            return cause.getClass().getSimpleName();
        }
        return message.split(
                "\\R(?=Scenario NO:|Scenario ACTION:|Sheet:|Testcase:|Row:|Keyword:|Object:|Application:)",
                2
        )[0].trim();
    }

    private String withStepContext(String message, ResolvedStepContext step) {
        if (step == null || !hasExecutionIdentity(step) || containsStepContext(message)) {
            return message;
        }
        String context = new ErrorContext()
                .scenarioNo(step.getScenarioNo())
                .scenarioAction(step.getScenarioAction())
                .sheet(step.getSheetName())
                .testcase(step.getTestcaseName())
                .row(step.getExcelRow())
                .keyword(step.getKeyword())
                .object(step.getObjectName())
                .application(step.getApplication())
                .render();
        return context.isBlank() ? message : message + System.lineSeparator() + context;
    }

    private boolean containsStepContext(String message) {
        return message != null
                && (message.contains("Scenario NO:") || message.contains("Scenario ACTION:"));
    }

    private boolean hasExecutionIdentity(ResolvedStepContext step) {
        return !isBlank(step.getScenarioNo())
                || !isBlank(step.getScenarioAction())
                || !isBlank(step.getTestcaseName())
                || step.getExcelRow() > 0;
    }


    private record MethodResolution(ResolvedKeyword resolvedKeyword, Method method, Object target) {
    }
}
