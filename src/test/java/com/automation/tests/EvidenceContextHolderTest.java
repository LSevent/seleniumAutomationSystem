package com.automation.tests;

import com.automation.context.EvidenceContextHolder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class EvidenceContextHolderTest {

    @AfterMethod(alwaysRun = true)
    public void clearEvidenceContext() {
        EvidenceContextHolder.clear();
    }

    @Test
    public void addShouldRegisterEvidenceForCurrentStep() {
        EvidenceContextHolder.start();

        EvidenceContextHolder.add("screenshots/before-login.png");

        Assert.assertEquals(
                EvidenceContextHolder.getAll(),
                List.of("screenshots/before-login.png")
        );
    }

    @Test
    public void addShouldPreserveMultipleEvidencePathsInOrder() {
        EvidenceContextHolder.start();

        EvidenceContextHolder.add("screenshots/booking-part-1.png");
        EvidenceContextHolder.add("screenshots/booking-part-2.png");
        EvidenceContextHolder.add("screenshots/booking-part-3.png");

        Assert.assertEquals(
                EvidenceContextHolder.getAll(),
                List.of(
                        "screenshots/booking-part-1.png",
                        "screenshots/booking-part-2.png",
                        "screenshots/booking-part-3.png"
                )
        );
    }

    @Test
    public void startShouldBeginWithFreshEvidenceCollection() {
        EvidenceContextHolder.start();
        EvidenceContextHolder.add("screenshots/old-step.png");

        EvidenceContextHolder.start();

        Assert.assertTrue(EvidenceContextHolder.getAll().isEmpty());
    }

    @Test
    public void clearShouldRemoveEvidenceAfterSuccessfulFlow() {
        EvidenceContextHolder.start();
        EvidenceContextHolder.add("screenshots/success.png");

        EvidenceContextHolder.clear();

        Assert.assertTrue(EvidenceContextHolder.getAll().isEmpty());
    }

    @Test
    public void clearShouldRemoveEvidenceAfterFailedFlow() {
        try {
            EvidenceContextHolder.start();
            EvidenceContextHolder.add("screenshots/before-failure.png");
            throw new IllegalStateException("Simulated keyword failure.");
        } catch (IllegalStateException ignored) {
            // Simulates KeywordEngine failure handling.
        } finally {
            EvidenceContextHolder.clear();
        }

        Assert.assertTrue(EvidenceContextHolder.getAll().isEmpty());
    }

    @Test
    public void evidenceShouldRemainIsolatedBetweenThreads() throws InterruptedException {
        AtomicReference<List<String>> otherThreadBeforeStart = new AtomicReference<>();
        AtomicReference<List<String>> otherThreadEvidence = new AtomicReference<>();

        EvidenceContextHolder.start();
        EvidenceContextHolder.add("screenshots/main-thread.png");

        Thread otherFlow = new Thread(() -> {
            otherThreadBeforeStart.set(EvidenceContextHolder.getAll());
            EvidenceContextHolder.start();
            EvidenceContextHolder.add("screenshots/other-thread.png");
            otherThreadEvidence.set(EvidenceContextHolder.getAll());
            EvidenceContextHolder.clear();
        });

        otherFlow.start();
        otherFlow.join();

        Assert.assertTrue(otherThreadBeforeStart.get().isEmpty());
        Assert.assertEquals(otherThreadEvidence.get(), List.of("screenshots/other-thread.png"));
        Assert.assertEquals(EvidenceContextHolder.getAll(), List.of("screenshots/main-thread.png"));
    }

    @Test
    public void addWithoutStartShouldFailClearly() {
        IllegalStateException exception = Assert.expectThrows(
                IllegalStateException.class,
                () -> EvidenceContextHolder.add("screenshots/unowned.png")
        );

        Assert.assertEquals(
                exception.getMessage(),
                "Evidence context is not available. Evidence collection must be started before adding evidence."
        );
    }

    @Test
    public void getAllShouldReturnImmutableSnapshot() {
        EvidenceContextHolder.start();
        EvidenceContextHolder.add("screenshots/immutable.png");

        List<String> evidence = EvidenceContextHolder.getAll();

        Assert.expectThrows(
                UnsupportedOperationException.class,
                () -> evidence.add("screenshots/not-allowed.png")
        );
    }
}
