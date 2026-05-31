package com.automation.utils;

import com.automation.constants.FrameworkConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class DataProviderUtil {

    private DataProviderUtil() {
    }

    @DataProvider(name = "validLoginData", parallel = true)
    public static Object[][] validLoginData() {
        return credentialsToArray(loadLoginData().getValidLogins(), false);
    }

    @DataProvider(name = "invalidLoginData", parallel = true)
    public static Object[][] invalidLoginData() {
        return credentialsToArray(loadLoginData().getInvalidLogins(), true);
    }

    @DataProvider(name = "emptyCredentialsData", parallel = true)
    public static Object[][] emptyCredentialsData() {
        return credentialsToArray(loadLoginData().getEmptyCredentials(), true);
    }

    private static LoginTestData loadLoginData() {
        try (InputStream inputStream = DataProviderUtil.class.getClassLoader().getResourceAsStream(FrameworkConstants.LOGIN_TEST_DATA_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Login test data file was not found: " + FrameworkConstants.LOGIN_TEST_DATA_RESOURCE);
            }
            return new ObjectMapper().readValue(inputStream, LoginTestData.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read login test data.", exception);
        }
    }

    private static Object[][] credentialsToArray(List<LoginCredentials> credentials, boolean includeExpectedMessage) {
        if (credentials == null || credentials.isEmpty()) {
            return new Object[0][0];
        }

        Object[][] data = new Object[credentials.size()][includeExpectedMessage ? 3 : 2];
        for (int index = 0; index < credentials.size(); index++) {
            LoginCredentials credential = credentials.get(index);
            data[index][0] = credential.getUsername();
            data[index][1] = credential.getPassword();
            if (includeExpectedMessage) {
                data[index][2] = credential.getExpectedErrorMessage();
            }
        }
        return data;
    }

    public static class LoginTestData {
        private List<LoginCredentials> validLogins;
        private List<LoginCredentials> invalidLogins;
        private List<LoginCredentials> emptyCredentials;

        public List<LoginCredentials> getValidLogins() {
            return validLogins;
        }

        public void setValidLogins(List<LoginCredentials> validLogins) {
            this.validLogins = validLogins;
        }

        public List<LoginCredentials> getInvalidLogins() {
            return invalidLogins;
        }

        public void setInvalidLogins(List<LoginCredentials> invalidLogins) {
            this.invalidLogins = invalidLogins;
        }

        public List<LoginCredentials> getEmptyCredentials() {
            return emptyCredentials;
        }

        public void setEmptyCredentials(List<LoginCredentials> emptyCredentials) {
            this.emptyCredentials = emptyCredentials;
        }
    }

    public static class LoginCredentials {
        private String username;
        private String password;
        private String expectedErrorMessage;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getExpectedErrorMessage() {
            return expectedErrorMessage;
        }

        public void setExpectedErrorMessage(String expectedErrorMessage) {
            this.expectedErrorMessage = expectedErrorMessage;
        }
    }
}
