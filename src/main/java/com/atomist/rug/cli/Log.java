package com.atomist.rug.cli;

import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReporterUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.CommandLineOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.Optional;

public class Log implements Logger{

    // There is a performance hit to have a logger per clazz
    private static final Logger logger = LoggerFactory.getLogger(Log.class.getName());

    public void error(Throwable e) {
        newline();
        e.printStackTrace(System.err);
        logger.error(e.getMessage(), e);
    }

    @Override
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

    public Log(Class<?> clazz) {
    }

    public void error(String message, Object... tokens) {
        newline();
        info(Style.red(message), (Object[]) tokens);
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
            if (!CommandLineOptions.hasOption("O")) {
                System.out.println(message);
            }
            else {
                System.err.println(message);
            }
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(msg,t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isInfoEnabled();
    }

    @Override
    public void error(Marker marker, String msg) {
        logger.error(marker,msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        logger.error(marker,format,arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        logger.error(marker,format,arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        logger.error(marker,format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        logger.error(marker,msg,t);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        logger.trace(format,arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logger.trace(format,arg1,arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        logger.trace(format,arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logger.trace(msg,t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String msg) {
        logger.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        logger.trace(marker,format,arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        logger.trace(marker,format,arg1,arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        logger.trace(marker,format,argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        logger.trace(marker,msg,t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logger.debug(format,arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(format,arg1,arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug(format,arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(msg,t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        logger.debug(marker,msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        logger.debug(marker,format,arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        logger.debug(marker,format,arg1,arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        logger.debug(marker,format,arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        logger.debug(marker,msg,t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        logger.info(format,arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logger.info(format,arg1,arg2);
    }


    @Override
    public void info(String msg, Throwable t) {
        logger.info(msg,t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        logger.info(marker,msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        logger.info(marker,format,arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        logger.info(marker,format,arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        logger.info(marker,format,arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        logger.info(marker,msg,t);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logger.warn(format,arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn(format,arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(format,arg1,arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(msg,t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        logger.warn(marker,msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        logger.warn(marker,format,arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        logger.warn(marker,format,arg1,arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        logger.warn(marker,format,arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        logger.warn(marker,msg,t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        logger.error(format,arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logger.error(format,arg1,arg2);
    }
}
