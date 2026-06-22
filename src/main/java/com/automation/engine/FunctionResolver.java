package com.automation.engine;

import com.automation.base.BaseFunction;
import com.automation.context.StepContextHolder;
import com.automation.exceptions.ErrorContext;
import com.automation.exceptions.FrameworkException;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.FunctionSourceType;
import com.automation.models.ResolvedFunction;
import com.automation.models.ResolvedStepContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Optional;

public class FunctionResolver {

    private static final Logger LOGGER = LogManager.getLogger(FunctionResolver.class);
    private static final String SPECIFIC_FUNCTION_PACKAGE_PREFIX = "com.automation.functions.";
    private static final String SPECIFIC_FUNCTION_CLASS_SUFFIX = ".SpecificFunction";

    private final WebDriver driver;
    private final BaseFunction baseFunction;

    public FunctionResolver(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver must not be null.");
        }
        this.driver = driver;
        this.baseFunction = new BaseFunction(driver);
    }

    public ResolvedFunction resolve(String application, String keywordName) {
        return resolveInternal(
                application,
                keywordName,
                StepContextHolder.current().orElse(null)
        ).resolvedKeyword();
    }

    public WebDriver getDriver() {
        return driver;
    }

    public FunctionExecutionResult execute(String application, String keywordName) {
        ResolvedStepContext step = StepContextHolder.get();
        MethodResolution resolution = resolveInternal(application, keywordName, step);
        String keyword = keywordName.trim();

        try {
            resolution.method().invoke(resolution.target());
            String message = "Executed keyword '" + keyword + "' using "
                    + resolution.resolvedKeyword().getResolvedClassName() + ".";
            return new FunctionExecutionResult(
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
        String specificClassName = specificClassName(normalizedApplication);

        LOGGER.info("Resolving keyword '{}' for application '{}'.", keyword, normalizedApplication);

        Class<?> specificClass = loadSpecificFunctionClass(specificClassName, normalizedApplication);
        if (specificClass != null) {
            Optional<Method> specificMethod = findNoArgMethod(specificClass, keyword, true);
            if (specificMethod.isPresent()) {
                return specificResolution(specificClass, normalizedApplication, keyword, specificMethod.get());
            }
        }

        Optional<Method> baseMethod = findNoArgMethod(BaseFunction.class, keyword, true);
        if (baseMethod.isPresent()) {
            return baseResolution(normalizedApplication, keyword, baseMethod.get());
        }

        String missingKeywordMessage = "Keyword '" + keyword + "' not found in SpecificFunction for application '"
                + normalizedApplication + "' or BaseFunction.";
        throw new FrameworkException(withStepContext(missingKeywordMessage, step));
    }

    private MethodResolution specificResolution(
            Class<?> specificClass,
            String application,
            String keyword,
            Method method
    ) {
        Object target = createSpecificFunction(specificClass, application);
        ResolvedFunction resolvedKeyword = new ResolvedFunction(
                application,
                keyword,
                specificClass.getName(),
                FunctionSourceType.SPECIFIC,
                method.getName()
        );
        LOGGER.info("Selected SpecificFunction for keyword '{}': {}", keyword, specificClass.getName());
        return new MethodResolution(resolvedKeyword, method, target);
    }

    private MethodResolution baseResolution(String application, String keyword, Method method) {
        ResolvedFunction resolvedKeyword = new ResolvedFunction(
                application,
                keyword,
                BaseFunction.class.getName(),
                FunctionSourceType.BASE,
                method.getName()
        );
        LOGGER.info("Selected BaseFunction for keyword '{}'.", keyword);
        return new MethodResolution(resolvedKeyword, method, baseFunction);
    }

    private Class<?> loadSpecificFunctionClass(String className, String application) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            LOGGER.warn(
                    "SpecificFunction not found for application '{}'. Expected class: {}. Checking BaseFunction.",
                    application,
                    className
            );
            return null;
        }
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

    private Optional<Method> findNoArgMethod(Class<?> functionClass, String keyword, boolean declaredOnly) {
        Method[] methods = declaredOnly ? functionClass.getDeclaredMethods() : functionClass.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(keyword)
                    && Modifier.isPublic(method.getModifiers())
                    && method.getParameterCount() == 0) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    private RuntimeException keywordExecutionFailure(
            String keyword,
            ResolvedFunction resolvedKeyword,
            Throwable cause,
            ResolvedStepContext step
    ) {
        String message = "Failed to execute keyword '" + keyword + "' using "
                + simpleClassName(resolvedKeyword.getResolvedClassName()) + ". Cause: " + cause.getMessage();
        message = withStepContext(message, step);
        LOGGER.error(message, cause);
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

    private String specificClassName(String normalizedApplication) {
        return SPECIFIC_FUNCTION_PACKAGE_PREFIX + normalizedApplication + SPECIFIC_FUNCTION_CLASS_SUFFIX;
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


    private record MethodResolution(ResolvedFunction resolvedKeyword, Method method, Object target) {
    }
}
