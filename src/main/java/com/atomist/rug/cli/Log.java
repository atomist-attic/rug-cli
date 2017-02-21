package com.atomist.rug.cli;

import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReporterUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.CommandLineOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.Optional;

public class Log implements Logger {

    // There is a performance hit to have a logger per clazz
    private static final Logger logger = LoggerFactory.getLogger(Log.class.getName());

    public Log(Class<?> clazz) {

    }

    @Override
    public void debug(Marker marker, String msg) {
        logger.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        logger.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        logger.debug(marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        logger.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        logger.debug(marker, msg, t);
    }

    @Override
    public void debug(String msg) {
        println(msg);
        logger.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        println(format, arg);
        logger.debug(format, arg);
    }

    @Override
    public void debug(String format, Object... arguments) {
        println(format, arguments);
        logger.debug(format, arguments);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        println(format, arg1, arg2);
        logger.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String msg, Throwable t) {
        println(msg);
        logger.debug(msg, t);
    }

    @Override
    public void error(Marker marker, String msg) {
        logger.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        logger.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        logger.error(marker, format, arguments);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        logger.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        logger.error(marker, msg);
    }

    @Override
    public void error(String msg) {
        doError(msg);
    }

    @Override
    public void error(String format, Object arg) {
        doError(format, arg);
    }

    @Override
    public void error(String format, Object... arguments) {
        doError(format, arguments);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        doError(format, arg1, arg2);
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }

    public void error(Throwable e) {
        newline();
        e.printStackTrace(System.err);
        logger.error(e.getMessage(), e);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public void info(Marker marker, String msg) {
        logger.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        logger.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        logger.info(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        logger.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        logger.info(marker, msg, t);
    }

    @Override
    public void info(String msg) {
        println(msg);
        logger.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        println(format, arg);
        logger.info(format, arg);
    }

    @Override
    public void info(String format, Object... arguments) {
        println(format, arguments);
        logger.info(format, arguments);

    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        println(format, arg1, arg2);
        logger.info(format, arg1, arg2);
    }

    @Override
    public void info(String msg, Throwable t) {
        println(msg);
        logger.info(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        logger.trace(msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        logger.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        logger.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        logger.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        logger.trace(marker, msg, t);
    }

    @Override
    public void trace(String msg) {
        println(msg);
        logger.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        println(format, arg);
        logger.trace(format, arg);
    }

    @Override
    public void trace(String format, Object... arguments) {
        println(format, arguments);
        logger.trace(format, arguments);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        println(format, arg1, arg2);
        logger.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String msg, Throwable t) {
        println(msg);
        logger.trace(msg, t);
    }

    @Override
    public void warn(Marker marker, String msg) {
        logger.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        logger.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        logger.warn(marker, format, arguments);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        logger.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        logger.warn(marker, msg, t);
    }

    @Override
    public void warn(String msg) {
        println(msg);
        logger.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        println(format, arg);
        logger.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        println(format, arguments);
        logger.warn(format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        println(format, arg1, arg2);
        logger.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        println(msg);
        logger.warn(msg, t);
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

    private void doError(String message, Object... tokens) {
        logger.error(message, tokens);
        newline();
        println(Style.red(message), (Object[]) tokens);
    }

    private void println(String message, Object... tokens) {
        if (tokens == null || tokens.length == 0) {
            doPrintln(message);
        }
        else {
            doPrintln(String.format(message, (Object[]) tokens));
        }
    }

    private void doPrintln(String message) {
        Optional<ProgressReporter> indicator = ProgressReporterUtils.getActiveProgressReporter();
        if (indicator.isPresent()) {
            indicator.get().report(message);
        }
        else {
            if (!CommandLineOptions.hasOption("O")) {
                System.out.println(message);
            }
            else {
                System.err.println(message);
            }
        }
    }
}
