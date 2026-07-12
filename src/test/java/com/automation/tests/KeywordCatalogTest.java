package com.automation.tests;

import com.automation.base.BaseFunction;
import com.automation.engine.KeywordCatalog;
import com.automation.engine.KeywordRequirements;
import com.automation.models.KeywordSourceType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class KeywordCatalogTest {

    private final KeywordCatalog catalog = new KeywordCatalog();

    @Test
    public void shouldDiscoverSpecificKeywordBeforeBaseKeyword() {
        KeywordCatalog.KeywordDefinition definition = catalog.discover("resolverTest", "click").orElseThrow();

        Assert.assertEquals(definition.sourceType(), KeywordSourceType.SPECIFIC);
        Assert.assertEquals(
                definition.implementationClass().getName(),
                "com.automation.functions.RESOLVERTEST.SpecificFunction"
        );
        Assert.assertEquals(definition.requirements(), KeywordRequirements.OBJECT);
    }

    @Test
    public void shouldDiscoverBaseKeywordWhenSpecificKeywordDoesNotExist() {
        KeywordCatalog.KeywordDefinition definition = catalog.discover("BRS", "input").orElseThrow();

        Assert.assertEquals(definition.sourceType(), KeywordSourceType.BASE);
        Assert.assertEquals(definition.implementationClass(), BaseFunction.class);
        Assert.assertEquals(definition.requirements(), KeywordRequirements.OBJECT_AND_VALUE);
    }

    @Test
    public void shouldDiscoverApplicationSpecificRequirements() {
        KeywordCatalog.KeywordDefinition brs = catalog.discover("brs", "clickMultiValue").orElseThrow();
        KeywordCatalog.KeywordDefinition hris = catalog.discover("hris", "verifyEmployeeVisible").orElseThrow();

        Assert.assertEquals(brs.requirements(), KeywordRequirements.OBJECT_AND_VALUE);
        Assert.assertEquals(hris.requirements(), KeywordRequirements.OBJECT_AND_VALUE);
        Assert.assertTrue(catalog.hasRegisteredRequirements("BRS", "clickMultiValue", KeywordSourceType.SPECIFIC));
        Assert.assertTrue(catalog.hasRegisteredRequirements("HRIS", "verifyEmployeeVisible", KeywordSourceType.SPECIFIC));
    }

    @Test
    public void customNoArgKeywordShouldBeDiscoveredWithoutMandatoryRegistration() {
        KeywordCatalog.KeywordDefinition definition = catalog.discover("RESOLVERTEST", "preferNoArg").orElseThrow();

        Assert.assertEquals(definition.sourceType(), KeywordSourceType.SPECIFIC);
        Assert.assertEquals(definition.requirements(), KeywordRequirements.NONE);
        Assert.assertFalse(catalog.hasRegisteredRequirements(
                "RESOLVERTEST",
                "preferNoArg",
                KeywordSourceType.SPECIFIC
        ));
    }

    @Test
    public void unknownKeywordShouldNotBeDiscovered() {
        Assert.assertTrue(catalog.discover("BRS", "doesNotExist").isEmpty());
    }

    @Test
    public void redundantSynchronizationAndClickAliasesShouldNotBeExposed() {
        Assert.assertTrue(catalog.discover("BRS", "waitVisible").isEmpty());
        Assert.assertTrue(catalog.discover("BRS", "waitClickable").isEmpty());
        Assert.assertTrue(catalog.discover("BRS", "safeClick").isEmpty());
    }

    @Test
    public void everyBaseKeywordShouldHaveCentralRequirements() {
        for (Method method : BaseFunction.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && method.getParameterCount() == 0) {
                Assert.assertTrue(
                        catalog.hasRegisteredRequirements("BRS", method.getName(), KeywordSourceType.BASE),
                        "Missing centralized requirements for BaseFunction keyword: " + method.getName()
                );
            }
        }
    }
}
