package com.atomist.rug.cli;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReporterUtils;
import com.atomist.rug.cli.output.Style;

public class Log {
    
    // There is a performance hit to have a logger per clazz
    private static final Logger logger = LoggerFactory.getLogger(Log.class.getName());

    public Log(Class<?> clazz) {
    }

    public void error(String message, Object... tokens) {
        info(Style.red(message), (Object[]) tokens);
    }

    public void error(Throwable e) {
        e.printStackTrace(System.err);
        logger.error(e.getMessage(), e);
    }

    public void info(String message, Object... tokens) {
        if (tokens == null || tokens.length == 0) {
            println(message);
            logger.info(message);
        }
        else {
            String format = String.format(message, (Object[]) tokens);
            println(format);
            logger.info(format);
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
