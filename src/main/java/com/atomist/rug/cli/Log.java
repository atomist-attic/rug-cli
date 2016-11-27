package com.atomist.rug.cli;

import java.util.Optional;

import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReporterUtils;
import com.atomist.rug.cli.output.Style;

public class Log {

    public Log(Class<?> clazz) {
    }

    public void error(String message, Object... tokens) {
        info(Style.red(message), (Object[]) tokens);
    }

    public void error(Throwable e) {
        // TODO CD only print stacktrace if -X
        e.printStackTrace(System.err);
    }

    public void info(String message, Object... tokens) {
        if (tokens == null || tokens.length == 0) {
            println(message);
        }
        else {
            String format = String.format(message, (Object[]) tokens);
            println(format);
        }
    }

    public void newline() {
        Optional<ProgressReporter> indicator = ProgressReporterUtils.getActiveProgressReporter();
        if (indicator.isPresent()) {
            info("$");
        }
        else {
            info("");
        }
    }

    private void println(String message) {
        Optional<ProgressReporter> indicator = ProgressReporterUtils.getActiveProgressReporter();
        if (indicator.isPresent()) {
            indicator.get().report(message);
        }
        else {
            System.out.println(message);
        }
    }

}
