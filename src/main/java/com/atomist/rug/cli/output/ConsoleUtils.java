package com.atomist.rug.cli.output;

public abstract class ConsoleUtils {

    public static int width() {
        String columns = System.getenv("COLUMNS");
        int width = 80;
        if (columns != null) {
            width = Integer.valueOf(columns);
        }
        // we keep 2 as buffer
        return width - 2;
    }

    public static void configureStreams() {
        System.setOut(new ProgressReportingPrintStream(System.out));
        System.setErr(new ProgressReportingPrintStream(System.err));
    }

}
