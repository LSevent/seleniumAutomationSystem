package com.automation.engine;

import com.automation.base.BaseFunction;
import com.automation.models.KeywordSourceType;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class KeywordCatalog {

    private static final String SPECIFIC_FUNCTION_PACKAGE_PREFIX = "com.automation.functions.";
    private static final String SPECIFIC_FUNCTION_CLASS_SUFFIX = ".SpecificFunction";

    private static final Map<String, KeywordRequirements> COMMON_REQUIREMENTS = Map.ofEntries(
            Map.entry("openurl", KeywordRequirements.VALUE),
            Map.entry("click", KeywordRequirements.OBJECT),
            Map.entry("input", KeywordRequirements.OBJECT_AND_VALUE),
            Map.entry("clear", KeywordRequirements.OBJECT),
            Map.entry("select", KeywordRequirements.OBJECT_AND_VALUE),
            Map.entry("verifydisplayed", KeywordRequirements.OBJECT),
            Map.entry("verifynotdisplayed", KeywordRequirements.OBJECT),
            Map.entry("verifytext", KeywordRequirements.OBJECT_AND_VALUE),
            Map.entry("verifytextcontains", KeywordRequirements.OBJECT_AND_VALUE),
            Map.entry("verifyurlcontains", KeywordRequirements.VALUE),
            Map.entry("verifytitle", KeywordRequirements.VALUE),
            Map.entry("verifytitlecontains", KeywordRequirements.VALUE),
            Map.entry("waitvisible", KeywordRequirements.OBJECT),
            Map.entry("waitclickable", KeywordRequirements.OBJECT),
            Map.entry("scrolltoelement", KeywordRequirements.OBJECT),
            Map.entry("safeclick", KeywordRequirements.OBJECT),
            Map.entry("pressenter", KeywordRequirements.OBJECT),
            Map.entry("screenshot", KeywordRequirements.NONE),
            Map.entry("screenshotpartbyobject", KeywordRequirements.OBJECT),
            Map.entry("screenshotfullpart", KeywordRequirements.NONE)
    );

    private static final Map<String, KeywordRequirements> SPECIFIC_REQUIREMENTS = Map.ofEntries(
            Map.entry(specificKey("BRS", "selectRoomByName"), KeywordRequirements.OBJECT),
            Map.entry(specificKey("BRS", "verifyBookingCreated"), KeywordRequirements.OBJECT_AND_VALUE),
            Map.entry(specificKey("HRIS", "verifyEmployeeVisible"), KeywordRequirements.OBJECT_AND_VALUE)
    );

    public Optional<KeywordDefinition> discover(String application, String keywordName) {
        if (isBlank(application) || isBlank(keywordName)) {
            return Optional.empty();
        }

        String normalizedApplication = normalizeApplication(application);
        String keyword = keywordName.trim();
        Class<?> specificClass = loadSpecificFunctionClass(normalizedApplication);

        if (specificClass != null) {
            Optional<Method> specificMethod = findPublicNoArgumentMethod(specificClass, keyword);
            if (specificMethod.isPresent()) {
                return Optional.of(definition(
                        normalizedApplication,
                        keyword,
                        specificClass,
                        specificMethod.get(),
                        KeywordSourceType.SPECIFIC
                ));
            }
        }

        return findPublicNoArgumentMethod(BaseFunction.class, keyword)
                .map(method -> definition(
                        normalizedApplication,
                        keyword,
                        BaseFunction.class,
                        method,
                        KeywordSourceType.BASE
                ));
    }

    public KeywordRequirements requirementsFor(
            String application,
            String keywordName,
            KeywordSourceType sourceType
    ) {
        String normalizedKeyword = normalizeKeyword(keywordName);
        if (sourceType == KeywordSourceType.SPECIFIC) {
            KeywordRequirements specificRequirements = SPECIFIC_REQUIREMENTS.get(
                    specificKey(normalizeApplication(application), normalizedKeyword)
            );
            if (specificRequirements != null) {
                return specificRequirements;
            }
        }
        return COMMON_REQUIREMENTS.getOrDefault(normalizedKeyword, KeywordRequirements.NONE);
    }

    public boolean hasRegisteredRequirements(
            String application,
            String keywordName,
            KeywordSourceType sourceType
    ) {
        String normalizedKeyword = normalizeKeyword(keywordName);
        if (sourceType == KeywordSourceType.SPECIFIC
                && SPECIFIC_REQUIREMENTS.containsKey(specificKey(normalizeApplication(application), normalizedKeyword))) {
            return true;
        }
        return COMMON_REQUIREMENTS.containsKey(normalizedKeyword);
    }

    private KeywordDefinition definition(
            String application,
            String keyword,
            Class<?> implementationClass,
            Method method,
            KeywordSourceType sourceType
    ) {
        return new KeywordDefinition(
                application,
                keyword,
                implementationClass,
                method,
                sourceType,
                requirementsFor(application, keyword, sourceType)
        );
    }

    private Class<?> loadSpecificFunctionClass(String application) {
        try {
            return Class.forName(
                    SPECIFIC_FUNCTION_PACKAGE_PREFIX + application + SPECIFIC_FUNCTION_CLASS_SUFFIX,
                    false,
                    Thread.currentThread().getContextClassLoader()
            );
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private Optional<Method> findPublicNoArgumentMethod(Class<?> type, String keyword) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(keyword)
                    && Modifier.isPublic(method.getModifiers())
                    && method.getParameterCount() == 0) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    private static String specificKey(String application, String keywordName) {
        return normalizeApplication(application) + ":" + normalizeKeyword(keywordName);
    }

    private static String normalizeApplication(String application) {
        return application == null ? "" : application.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeKeyword(String keywordName) {
        return keywordName == null ? "" : keywordName.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record KeywordDefinition(
            String application,
            String keywordName,
            Class<?> implementationClass,
            Method method,
            KeywordSourceType sourceType,
            KeywordRequirements requirements
    ) {
    }
}
