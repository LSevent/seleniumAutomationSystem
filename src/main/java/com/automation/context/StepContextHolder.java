package com.automation.context;

import com.automation.exceptions.FrameworkException;
import com.automation.models.ResolvedStepContext;

import java.util.Optional;

public final class StepContextHolder {

    private static final ThreadLocal<ResolvedStepContext> STEP_CONTEXT = new ThreadLocal<>();

    private StepContextHolder() {
    }

    public static void set(ResolvedStepContext context) {
        STEP_CONTEXT.set(context);
    }

    public static ResolvedStepContext get() {
        ResolvedStepContext context = STEP_CONTEXT.get();
        if (context == null) {
            throw new FrameworkException("Step context is not available. Keyword must be executed through KeywordEngine.");
        }
        return context;
    }

    public static Optional<ResolvedStepContext> current() {
        return Optional.ofNullable(STEP_CONTEXT.get());
    }

    public static void clear() {
        STEP_CONTEXT.remove();
    }
}
