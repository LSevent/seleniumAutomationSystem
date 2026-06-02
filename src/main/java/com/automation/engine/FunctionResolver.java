package com.automation.engine;

import com.automation.base.BaseFunction;
import com.automation.models.FunctionExecutionResult;
import com.automation.models.FunctionSourceType;
import com.automation.models.ResolvedFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class FunctionResolver {

    private static final Logger LOGGER = LogManager.getLogger(FunctionResolver.class);
    private static final String SPECIFIC_FUNCTION_PACKAGE_PREFIX = "com.automation.functions.";
    private static final String SPECIFIC_FUNCTION_CLASS_SUFFIX = ".SpecificFunction";
    private static final Map<String, ArgumentStyle> KNOWN_ARGUMENT_STYLES = Map.ofEntries(
            Map.entry("openUrl", ArgumentStyle.VALUE_ONLY),
            Map.entry("verifyUrlContains", ArgumentStyle.VALUE_ONLY),
            Map.entry("verifyTitle", ArgumentStyle.VALUE_ONLY),
            Map.entry("verifyTitleContains", ArgumentStyle.VALUE_ONLY),
            Map.entry("waitForApplicationReady", ArgumentStyle.NO_ARGS),
            Map.entry("click", ArgumentStyle.XPATH_ONLY),
            Map.entry("safeClick", ArgumentStyle.XPATH_ONLY),
            Map.entry("clear", ArgumentStyle.XPATH_ONLY),
            Map.entry("getText", ArgumentStyle.XPATH_ONLY),
            Map.entry("verifyDisplayed", ArgumentStyle.XPATH_ONLY),
            Map.entry("waitVisible", ArgumentStyle.XPATH_ONLY),
            Map.entry("waitClickable", ArgumentStyle.XPATH_ONLY),
            Map.entry("scrollToElement", ArgumentStyle.XPATH_ONLY),
            Map.entry("pressEnter", ArgumentStyle.XPATH_ONLY),
            Map.entry("isDisplayed", ArgumentStyle.XPATH_ONLY),
            Map.entry("isNotDisplayed", ArgumentStyle.XPATH_ONLY),
            Map.entry("input", ArgumentStyle.XPATH_AND_VALUE),
            Map.entry("verifyText", ArgumentStyle.XPATH_AND_VALUE),
            Map.entry("verifyTextContains", ArgumentStyle.XPATH_AND_VALUE),
            Map.entry("selectRoomByName", ArgumentStyle.XPATH_AND_VALUE),
            Map.entry("verifyBookingCreated", ArgumentStyle.XPATH_AND_VALUE),
            Map.entry("verifyEmployeeVisible", ArgumentStyle.XPATH_AND_VALUE)
    );

    private final WebDriver driver;
    private final BaseFunction baseFunction;

    public FunctionResolver(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver must not be null.");
        }
        this.driver = driver;
        this.baseFunction = new BaseFunction(driver);
    }

    public ResolvedFunction resolve(String application, String functionName) {
        return resolveInternal(application, functionName).resolvedFunction();
    }

    public FunctionExecutionResult execute(
            String application,
            String functionName,
            String resolvedXpath,
            String resolvedValue
    ) {
        MethodResolution resolution = resolveInternal(application, functionName);
        validateArguments(functionName.trim(), resolvedXpath, resolvedValue);

        LOGGER.info(
                "Executing keyword '{}' using {}.",
                functionName.trim(),
                resolution.resolvedFunction().getResolvedClassName()
        );

        try {
            Object[] arguments = buildArguments(functionName.trim(), resolvedXpath, resolvedValue, resolution.method());
            resolution.method().invoke(resolution.target(), arguments);
            String message = "Executed keyword '" + functionName.trim() + "' using "
                    + resolution.resolvedFunction().getResolvedClassName() + ".";
            LOGGER.info(message);
            return new FunctionExecutionResult(
                    resolution.resolvedFunction().getApplication(),
                    resolution.resolvedFunction().getFunctionName(),
                    resolution.resolvedFunction().getResolvedClassName(),
                    resolution.resolvedFunction().getSourceType(),
                    true,
                    message
            );
        } catch (IllegalAccessException exception) {
            throw keywordExecutionFailure(functionName.trim(), resolution.resolvedFunction(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw keywordExecutionFailure(functionName.trim(), resolution.resolvedFunction(), cause);
        }
    }

    private MethodResolution resolveInternal(String application, String functionName) {
        validateFunctionName(functionName);
        String keyword = functionName.trim();
        String normalizedApplication = normalizeApplication(application, keyword);
        String specificClassName = specificClassName(normalizedApplication);

        LOGGER.info("Resolving keyword '{}' for application '{}'.", keyword, normalizedApplication);

        Class<?> specificClass = loadSpecificFunctionClass(specificClassName, normalizedApplication);
        if (specificClass != null) {
            Optional<Method> specificMethod = findKeywordMethod(specificClass, keyword, true);
            if (specificMethod.isPresent()) {
                Object target = createSpecificFunction(specificClass, normalizedApplication);
                ResolvedFunction resolvedFunction = new ResolvedFunction(
                        normalizedApplication,
                        keyword,
                        specificClass.getName(),
                        FunctionSourceType.SPECIFIC,
                        specificMethod.get().getName()
                );
                LOGGER.info("Selected SpecificFunction for keyword '{}': {}", keyword, specificClass.getName());
                return new MethodResolution(resolvedFunction, specificMethod.get(), target);
            }
        }

        Optional<Method> baseMethod = findKeywordMethod(BaseFunction.class, keyword, false);
        if (baseMethod.isPresent()) {
            ResolvedFunction resolvedFunction = new ResolvedFunction(
                    normalizedApplication,
                    keyword,
                    BaseFunction.class.getName(),
                    FunctionSourceType.BASE,
                    baseMethod.get().getName()
            );
            LOGGER.info("Fallback to BaseFunction for keyword '{}'.", keyword);
            return new MethodResolution(resolvedFunction, baseMethod.get(), baseFunction);
        }

        String missingKeywordMessage = "Keyword '" + keyword + "' not found in SpecificFunction for application '"
                + normalizedApplication + "' or BaseFunction.";
        if (specificClass == null) {
            throw new IllegalArgumentException("SpecificFunction not found for application '" + normalizedApplication
                    + "'. Expected class: " + specificClassName + ". " + missingKeywordMessage);
        }
        throw new IllegalArgumentException(missingKeywordMessage);
    }

    private Class<?> loadSpecificFunctionClass(String className, String application) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            LOGGER.warn(
                    "SpecificFunction not found for application '{}'. Expected class: {}. Trying BaseFunction fallback.",
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
            throw new IllegalStateException("SpecificFunction for application '" + application
                    + "' must have a constructor that accepts WebDriver.", exception);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not create SpecificFunction for application '" + application + "'.", exception);
        }
    }

    private Optional<Method> findKeywordMethod(Class<?> functionClass, String keyword, boolean declaredOnly) {
        Method[] methods = declaredOnly ? functionClass.getDeclaredMethods() : functionClass.getMethods();
        ArgumentStyle argumentStyle = KNOWN_ARGUMENT_STYLES.get(keyword);

        if (argumentStyle != null) {
            return findMethod(methods, keyword, argumentStyle.parameterCount());
        }

        return findMethod(methods, keyword, 2)
                .or(() -> findMethod(methods, keyword, 1))
                .or(() -> findMethod(methods, keyword, 0));
    }

    private Optional<Method> findMethod(Method[] methods, String keyword, int parameterCount) {
        for (Method method : methods) {
            if (method.getName().equals(keyword)
                    && Modifier.isPublic(method.getModifiers())
                    && method.getParameterCount() == parameterCount
                    && hasOnlyStringParameters(method)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    private boolean hasOnlyStringParameters(Method method) {
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (!String.class.equals(parameterType)) {
                return false;
            }
        }
        return true;
    }

    private Object[] buildArguments(String keyword, String resolvedXpath, String resolvedValue, Method method) {
        ArgumentStyle argumentStyle = KNOWN_ARGUMENT_STYLES.get(keyword);
        if (argumentStyle != null) {
            return switch (argumentStyle) {
                case NO_ARGS -> new Object[0];
                case XPATH_ONLY -> new Object[]{resolvedXpath.trim()};
                case VALUE_ONLY -> new Object[]{resolvedValue.trim()};
                case XPATH_AND_VALUE -> new Object[]{resolvedXpath.trim(), resolvedValue.trim()};
            };
        }

        return switch (method.getParameterCount()) {
            case 0 -> new Object[0];
            case 1 -> new Object[]{firstNonBlank(resolvedXpath, resolvedValue)};
            case 2 -> new Object[]{resolvedXpath, resolvedValue};
            default -> throw new IllegalArgumentException("Unsupported method signature for keyword '" + keyword + "'.");
        };
    }

    private void validateArguments(String keyword, String resolvedXpath, String resolvedValue) {
        ArgumentStyle argumentStyle = KNOWN_ARGUMENT_STYLES.get(keyword);
        if (argumentStyle == null) {
            return;
        }

        if (argumentStyle.requiresXpath() && isBlank(resolvedXpath)) {
            throw new IllegalArgumentException("XPath is required for keyword '" + keyword + "'.");
        }
        if (argumentStyle.requiresValue() && isBlank(resolvedValue)) {
            throw new IllegalArgumentException("Value is required for keyword '" + keyword + "'.");
        }
    }

    private RuntimeException keywordExecutionFailure(String keyword, ResolvedFunction resolvedFunction, Throwable cause) {
        String message = "Failed to execute keyword '" + keyword + "' using "
                + resolvedFunction.getResolvedClassName() + ". Cause: " + cause.getMessage();
        LOGGER.error(message, cause);
        if (cause instanceof AssertionError) {
            throw new AssertionError(message, cause);
        }
        return new IllegalStateException(message, cause);
    }

    private String normalizeApplication(String application, String keyword) {
        if (isBlank(application)) {
            throw new IllegalArgumentException("Application is required to resolve keyword '" + keyword + "'.");
        }
        return application.trim().toUpperCase(Locale.ROOT);
    }

    private void validateFunctionName(String functionName) {
        if (isBlank(functionName)) {
            throw new IllegalArgumentException("Function name is required.");
        }
    }

    private String specificClassName(String normalizedApplication) {
        return SPECIFIC_FUNCTION_PACKAGE_PREFIX + normalizedApplication + SPECIFIC_FUNCTION_CLASS_SUFFIX;
    }

    private String firstNonBlank(String resolvedXpath, String resolvedValue) {
        if (!isBlank(resolvedXpath)) {
            return resolvedXpath.trim();
        }
        if (!isBlank(resolvedValue)) {
            return resolvedValue.trim();
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private enum ArgumentStyle {
        NO_ARGS(false, false, 0),
        XPATH_ONLY(true, false, 1),
        VALUE_ONLY(false, true, 1),
        XPATH_AND_VALUE(true, true, 2);

        private final boolean requiresXpath;
        private final boolean requiresValue;
        private final int parameterCount;

        ArgumentStyle(boolean requiresXpath, boolean requiresValue, int parameterCount) {
            this.requiresXpath = requiresXpath;
            this.requiresValue = requiresValue;
            this.parameterCount = parameterCount;
        }

        private boolean requiresXpath() {
            return requiresXpath;
        }

        private boolean requiresValue() {
            return requiresValue;
        }

        private int parameterCount() {
            return parameterCount;
        }
    }

    private record MethodResolution(ResolvedFunction resolvedFunction, Method method, Object target) {
    }
}
