package com.automation.context;

import com.automation.reports.ExcelReportConfig;
import com.automation.services.ScreenshotService;

public final class ScreenshotContextHolder {

    private static final ThreadLocal<ScreenshotContext> CURRENT_SCREENSHOT_CONTEXT = new ThreadLocal<>();

    private ScreenshotContextHolder() {
    }

    public static void set(ScreenshotService screenshotService, ExcelReportConfig reportConfig) {
        if (screenshotService == null) {
            throw new IllegalArgumentException("ScreenshotService must not be null.");
        }
        CURRENT_SCREENSHOT_CONTEXT.set(new ScreenshotContext(
                screenshotService,
                reportConfig == null ? ExcelReportConfig.fromConfig() : reportConfig
        ));
    }

    public static ScreenshotService service() {
        return context().screenshotService();
    }

    public static ExcelReportConfig reportConfig() {
        return context().reportConfig();
    }

    public static boolean isManualScreenshotEnabled() {
        return reportConfig().isManualScreenshotEnabled();
    }

    public static void clear() {
        CURRENT_SCREENSHOT_CONTEXT.remove();
    }

    private static ScreenshotContext context() {
        ScreenshotContext context = CURRENT_SCREENSHOT_CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException(
                    "Screenshot context is not available. Screenshot keywords must be executed through KeywordEngine."
            );
        }
        return context;
    }

    private record ScreenshotContext(
            ScreenshotService screenshotService,
            ExcelReportConfig reportConfig
    ) {
    }
}
