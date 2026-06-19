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
        Optional<ResolvedStepContext> currentStep = StepContextHolder.current();
        return resolveInternal(
                application,
                functionName,
                currentStep.orElse(null),
                currentStep.isPresent()
        ).resolvedFunction();
    }

    public WebDriver getDriver() {
        return driver;
    }

    public FunctionExecutionResult execute(
            String application,
            String functionName,
            String resolvedXpath,
            String resolvedValue
    ) {
        Optional<ResolvedStepContext> currentStep = StepContextHolder.current();
        ResolvedStepContext step = currentStep.orElseGet(
                () -> compatibilityContext(application, functionName, resolvedXpath, resolvedValue)
        );
        MethodResolution resolution = resolveInternal(
                application,
                functionName,
                step,
                currentStep.isPresent()
        );
        String keyword = functionName.trim();
        validateArguments(keyword, step, resolution.method());

        LOGGER.info(
                "Executing keyword '{}' using {}.",
                keyword,
                resolution.resolvedFunction().getResolvedClassName()
        );

        try {
            Object[] arguments = buildArguments(keyword, step, resolution.method());
            resolution.method().invoke(resolution.target(), arguments);
            String message = "Executed keyword '" + keyword + "' using "
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
            throw keywordExecutionFailure(keyword, resolution.resolvedFunction(), exception, step);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw keywordExecutionFailure(keyword, resolution.resolvedFunction(), cause, step);
        }
    }

    private MethodResolution resolveInternal(
            String application,
            String functionName,
            ResolvedStepContext step,
            boolean preferNoArg
    ) {
        validateFunctionName(functionName);
        String keyword = functionName.trim();
        String normalizedApplication = normalizeApplication(application, keyword);
        String specificClassName = specificClassName(normalizedApplication);

        LOGGER.info("Resolving keyword '{}' for application '{}'.", keyword, normalizedApplication);

        Class<?> specificClass = loadSpecificFunctionClass(specificClassName, normalizedApplication);
        if (preferNoArg) {
            if (specificClass != null) {
                Optional<Method> noArgSpecificMethod = findNoArgMethod(specificClass, keyword, true);
                if (noArgSpecificMethod.isPresent()) {
                    return specificResolution(specificClass, normalizedApplication, keyword, noArgSpecificMethod.get());
                }
            }

            Optional<Method> noArgBaseMethod = findNoArgMethod(BaseFunction.class, keyword, false);
            if (noArgBaseMethod.isPresent()) {
                return baseResolution(normalizedApplication, keyword, noArgBaseMethod.get());
            }
        }

        if (specificClass != null) {
            Optional<Method> legacySpecificMethod = findLegacyMethod(specificClass, keyword, true);
            if (legacySpecificMethod.isPresent()) {
                return specificResolution(specificClass, normalizedApplication, keyword, legacySpecificMethod.get());
            }
        }

        Optional<Method> legacyBaseMethod = findLegacyMethod(BaseFunction.class, keyword, false);
        if (legacyBaseMethod.isPresent()) {
            return baseResolution(normalizedApplication, keyword, legacyBaseMethod.get());
        }

        if (!preferNoArg) {
            if (specificClass != null) {
                Optional<Method> noArgSpecificMethod = findNoArgMethod(specificClass, keyword, true);
                if (noArgSpecificMethod.isPresent()) {
                    return specificResolution(specificClass, normalizedApplication, keyword, noArgSpecificMethod.get());
                }
            }

            Optional<Method> noArgBaseMethod = findNoArgMethod(BaseFunction.class, keyword, false);
            if (noArgBaseMethod.isPresent()) {
                return baseResolution(normalizedApplication, keyword, noArgBaseMethod.get());
            }
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
        ResolvedFunction resolvedFunction = new ResolvedFunction(
                application,
                keyword,
                specificClass.getName(),
                FunctionSourceType.SPECIFIC,
                method.getName()
        );
        LOGGER.info("Selected SpecificFunction for keyword '{}': {}", keyword, specificClass.getName());
        return new MethodResolution(resolvedFunction, method, target);
    }

    private MethodResolution baseResolution(String application, String keyword, Method method) {
        ResolvedFunction resolvedFunction = new ResolvedFunction(
                application,
                keyword,
                BaseFunction.class.getName(),
                FunctionSourceType.BASE,
                method.getName()
        );
        LOGGER.info("Fallback to BaseFunction for keyword '{}'.", keyword);
        return new MethodResolution(resolvedFunction, method, baseFunction);
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
            throw new FrameworkException("SpecificFunction for application '" + application
                    + "' must have a constructor that accepts WebDriver.", exception);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new FrameworkException("Could not create SpecificFunction for application '" + application + "'.", exception);
        }
    }

    private Optional<Method> findNoArgMethod(Class<?> functionClass, String keyword, boolean declaredOnly) {
        return findMethod(functionClass, keyword, declaredOnly, MethodSignature.NO_ARGS);
    }

    private Optional<Method> findLegacyMethod(Class<?> functionClass, String keyword, boolean declaredOnly) {
        ArgumentStyle argumentStyle = KNOWN_ARGUMENT_STYLES.get(keyword);

        if (argumentStyle != null && argumentStyle.parameterCount() > 0) {
            Optional<Method> knownStyleMethod = findMethod(
                    functionClass,
                    keyword,
                    declaredOnly,
                    MethodSignature.forStringCount(argumentStyle.parameterCount())
            );
            if (knownStyleMethod.isPresent()) {
                return knownStyleMethod;
            }
        }

        return findMethod(functionClass, keyword, declaredOnly, MethodSignature.TWO_STRINGS)
                .or(() -> findMethod(functionClass, keyword, declaredOnly, MethodSignature.ONE_STRING))
                .or(() -> findMethod(functionClass, keyword, declaredOnly, MethodSignature.CONTEXT_FALLBACK));
    }

    private Optional<Method> findMethod(
            Class<?> functionClass,
            String keyword,
            boolean declaredOnly,
            MethodSignature signature
    ) {
        Method[] methods = declaredOnly ? functionClass.getDeclaredMethods() : functionClass.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(keyword)
                    && Modifier.isPublic(method.getModifiers())
                    && signature.matches(method)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    private Object[] buildArguments(String keyword, ResolvedStepContext step, Method method) {
        if (method.getParameterCount() == 0) {
            return new Object[0];
        }
        if (MethodSignature.CONTEXT_FALLBACK.matches(method)) {
            return new Object[]{step.getResolvedXPath(), step.getResolvedValue(), step};
        }

        ArgumentStyle argumentStyle = KNOWN_ARGUMENT_STYLES.get(keyword);
        if (method.getParameterCount() == 1) {
            if (argumentStyle == ArgumentStyle.XPATH_ONLY) {
                return new Object[]{safe(step.getResolvedXPath())};
            }
            if (argumentStyle == ArgumentStyle.VALUE_ONLY) {
                return new Object[]{safe(step.getResolvedValue())};
            }
            return new Object[]{firstNonBlank(step.getResolvedXPath(), step.getResolvedValue())};
        }
        if (method.getParameterCount() == 2) {
            return new Object[]{safe(step.getResolvedXPath()), safe(step.getResolvedValue())};
        }
        throw new IllegalArgumentException("Unsupported method signature for keyword '" + keyword + "'.");
    }

    private void validateArguments(String keyword, ResolvedStepContext step, Method method) {
        if (method.getParameterCount() == 0) {
            return;
        }

        ArgumentStyle argumentStyle = KNOWN_ARGUMENT_STYLES.get(keyword);
        if (argumentStyle == null) {
            return;
        }

        if (argumentStyle.requiresXpath() && isBlank(step.getResolvedXPath())) {
            throw new FrameworkException(withStepContext("XPath is required for keyword '" + keyword + "'.", step));
        }
        if (argumentStyle.requiresValue() && isBlank(step.getResolvedValue())) {
            throw new FrameworkException(withStepContext(requiredValueMessage(keyword), step));
        }
    }

    private RuntimeException keywordExecutionFailure(
            String keyword,
            ResolvedFunction resolvedFunction,
            Throwable cause,
            ResolvedStepContext step
    ) {
        String message = "Failed to execute keyword '" + keyword + "' using "
                + simpleClassName(resolvedFunction.getResolvedClassName()) + ". Cause: " + cause.getMessage();
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

    private void validateFunctionName(String functionName) {
        if (isBlank(functionName)) {
            throw new FrameworkException("Function name is required.");
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

    private String requiredValueMessage(String keyword) {
        return switch (keyword) {
            case "openUrl" -> "URL is required for keyword 'openUrl'.";
            case "verifyText", "verifyTextContains" -> "Expected text is required for keyword '" + keyword + "'.";
            case "verifyUrlContains" -> "Expected value is required for keyword 'verifyUrlContains'.";
            case "verifyTitle", "verifyTitleContains" -> "Expected title is required for keyword '" + keyword + "'.";
            default -> "Value is required for keyword '" + keyword + "'.";
        };
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
                .testcase(step.getTestcaseName())
                .row(step.getExcelRow())
                .function(step.getFunction())
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

    private ResolvedStepContext compatibilityContext(
            String application,
            String functionName,
            String resolvedXpath,
            String resolvedValue
    ) {
        return new ResolvedStepContext(
                "",
                "",
                "",
                "",
                "",
                0,
                0,
                0,
                safe(functionName),
                "",
                safe(application),
                "",
                safe(resolvedValue),
                safe(resolvedValue),
                safe(resolvedXpath),
                safe(resolvedXpath),
                ""
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
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

    private enum MethodSignature {
        NO_ARGS(new Class<?>[0]),
        ONE_STRING(String.class),
        TWO_STRINGS(String.class, String.class),
        CONTEXT_FALLBACK(String.class, String.class, ResolvedStepContext.class);

        private final Class<?>[] parameterTypes;

        MethodSignature(Class<?>... parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        private boolean matches(Method method) {
            return java.util.Arrays.equals(parameterTypes, method.getParameterTypes());
        }

        private static MethodSignature forStringCount(int parameterCount) {
            return switch (parameterCount) {
                case 1 -> ONE_STRING;
                case 2 -> TWO_STRINGS;
                default -> throw new IllegalArgumentException(
                        "Unsupported legacy String parameter count: " + parameterCount + "."
                );
            };
        }
    }

    private record MethodResolution(ResolvedFunction resolvedFunction, Method method, Object target) {
    }
}
