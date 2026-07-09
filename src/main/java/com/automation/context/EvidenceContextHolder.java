package com.automation.context;

import java.util.ArrayList;
import java.util.List;

public final class EvidenceContextHolder {

    private static final ThreadLocal<List<String>> EVIDENCE_PATHS = new ThreadLocal<>();

    private EvidenceContextHolder() {
    }

    public static void start() {
        EVIDENCE_PATHS.set(new ArrayList<>());
    }

    public static void add(String evidencePath) {
        if (evidencePath == null || evidencePath.isBlank()) {
            throw new IllegalArgumentException("Evidence path must not be blank.");
        }

        List<String> currentEvidence = EVIDENCE_PATHS.get();
        if (currentEvidence == null) {
            throw new IllegalStateException(
                    "Evidence context is not available. Evidence collection must be started before adding evidence."
            );
        }
        currentEvidence.add(evidencePath);
    }

    public static List<String> getAll() {
        List<String> currentEvidence = EVIDENCE_PATHS.get();
        return currentEvidence == null ? List.of() : List.copyOf(currentEvidence);
    }

    public static void clear() {
        EVIDENCE_PATHS.remove();
    }
}
