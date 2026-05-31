package com.automation.config;

import com.automation.constants.FrameworkConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigReader {

    private static final Properties PROPERTIES = new Properties();
    private static boolean loaded;

    private ConfigReader() {
    }

    public static synchronized void loadConfig() {
        if (loaded) {
            return;
        }

        try (InputStream inputStream = openConfigStream()) {
            PROPERTIES.load(inputStream);
            loaded = true;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load configuration from config.properties", exception);
        }
    }

    public static String getProperty(String key) {
        loadConfig();
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required configuration key: " + key);
        }
        return value.trim();
    }

    public static String getProperty(String key, String defaultValue) {
        loadConfig();
        String value = PROPERTIES.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    public static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Configuration key '" + key + "' must be a number. Actual value: " + value, exception);
        }
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getProperty(key, String.valueOf(defaultValue)));
    }

    private static InputStream openConfigStream() throws IOException {
        InputStream classpathConfig = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties");
        if (classpathConfig != null) {
            return classpathConfig;
        }

        Path configPath = Path.of(FrameworkConstants.CONFIG_FILE_PATH);
        if (Files.exists(configPath)) {
            return Files.newInputStream(configPath);
        }

        throw new IOException("config.properties was not found on the classpath or at " + configPath.toAbsolutePath());
    }
}
