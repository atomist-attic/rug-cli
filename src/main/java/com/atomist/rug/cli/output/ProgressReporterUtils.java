package com.atomist.rug.cli.output;

import java.util.Optional;

public class ProgressReporterUtils {

    private static InheritableThreadLocal<ProgressReporter> threadLocal = new InheritableThreadLocal<>();

    public static Optional<ProgressReporter> getActiveProgressReporter() {
        return Optional.ofNullable(threadLocal.get());
    }

    public static void removeActiveProgressReporter() {
        threadLocal.remove();
    }

    public static void setActiveProgressReporter(ProgressReporter progressReporter) {
        threadLocal.set(progressReporter);
    }
}
