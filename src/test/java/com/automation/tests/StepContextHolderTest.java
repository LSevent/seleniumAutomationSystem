package com.automation.tests;

import com.automation.context.StepContextHolder;
import com.automation.exceptions.FrameworkException;
import com.automation.models.ResolvedStepContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class StepContextHolderTest {

    private static final String MISSING_CONTEXT_MESSAGE =
            "Step context is not available. Keyword must be executed through KeywordEngine.";

    @AfterMethod(alwaysRun = true)
    public void clearContext() {
        StepContextHolder.clear();
    }

    @Test
    public void setAndGetShouldReturnCurrentContext() {
        ResolvedStepContext context = context("SC-001", 1);

        StepContextHolder.set(context);

        Assert.assertSame(StepContextHolder.get(), context);
    }

    @Test
    public void currentShouldReturnOptionalWithCurrentContext() {
        ResolvedStepContext context = context("SC-002", 2);

        StepContextHolder.set(context);

        Optional<ResolvedStepContext> current = StepContextHolder.current();
        Assert.assertTrue(current.isPresent());
        Assert.assertSame(current.orElseThrow(), context);
    }

    @Test
    public void clearShouldRemoveCurrentContext() {
        StepContextHolder.set(context("SC-003", 3));

        StepContextHolder.clear();

        Assert.assertTrue(StepContextHolder.current().isEmpty());
    }

    @Test
    public void getAfterClearShouldThrowFrameworkException() {
        StepContextHolder.set(context("SC-004", 4));
        StepContextHolder.clear();

        FrameworkException exception = Assert.expectThrows(
                FrameworkException.class,
                StepContextHolder::get
        );

        Assert.assertEquals(exception.getMessage(), MISSING_CONTEXT_MESSAGE);
    }

    @Test
    public void contextShouldNotLeakBetweenDifferentThreadFlows() throws InterruptedException {
        ResolvedStepContext mainThreadContext = context("SC-MAIN", 5);
        ResolvedStepContext otherThreadContext = context("SC-OTHER", 6);
        AtomicReference<Optional<ResolvedStepContext>> initialOtherThreadContext = new AtomicReference<>();
        AtomicReference<ResolvedStepContext> storedOtherThreadContext = new AtomicReference<>();

        StepContextHolder.set(mainThreadContext);
        Thread otherFlow = new Thread(() -> {
            initialOtherThreadContext.set(StepContextHolder.current());
            StepContextHolder.set(otherThreadContext);
            storedOtherThreadContext.set(StepContextHolder.get());
            StepContextHolder.clear();
        });

        otherFlow.start();
        otherFlow.join();

        Assert.assertTrue(initialOtherThreadContext.get().isEmpty());
        Assert.assertSame(storedOtherThreadContext.get(), otherThreadContext);
        Assert.assertSame(StepContextHolder.get(), mainThreadContext);
    }

    private ResolvedStepContext context(String scenarioNo, int stepNumber) {
        return new ResolvedStepContext(
                scenarioNo,
                "BOOKING",
                "Create a booking",
                "BOOKING",
                "Create booking successfully",
                3,
                4,
                stepNumber,
                "click",
                "btnSubmit",
                "BRS",
                "Submit booking",
                "",
                "",
                "//button[@id='submit']",
                "//button[@id='submit']",
                "com.automation.base.BaseFunction"
        );
    }
}
