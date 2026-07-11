package com.automation.engine;

public record KeywordRequirements(boolean objectRequired, boolean valueRequired) {

    public static final KeywordRequirements NONE = new KeywordRequirements(false, false);
    public static final KeywordRequirements OBJECT = new KeywordRequirements(true, false);
    public static final KeywordRequirements VALUE = new KeywordRequirements(false, true);
    public static final KeywordRequirements OBJECT_AND_VALUE = new KeywordRequirements(true, true);
}
