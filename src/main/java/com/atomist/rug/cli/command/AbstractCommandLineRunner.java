package com.atomist.rug.cli.command;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.cli.Log;

public abstract class AbstractCommandLineRunner {

    private Log log = new Log(getClass());

    public void runCommand(String[] args, CommandLine commandLine) {
        try {
            doRunCommand(args, commandLine);
        }
        catch (Throwable e) {
            // Extract root exception; cycle through nested exceptions to extract root cause
            e = extractRootCause(e);

            // Print stacktraces only if requested from the command line
            log.newline();
            if (commandLine.hasOption('X')) {
                log.error(e);
            }
            else {
                log.error(e.getMessage());
            }
            System.exit(1);
        }
    }

    protected abstract void doRunCommand(String[] args, CommandLine commandLine);

    private Throwable extractRootCause(Throwable t) {
        if (t instanceof InvocationTargetException) {
            return extractRootCause(((InvocationTargetException) t).getTargetException());
        }
        else if (t instanceof CommandException) {
            return t;
        }
        else if (t instanceof RuntimeException) {
            if (t.getCause() != null) {
                return extractRootCause(t.getCause());
            }
        }
        return t;
    }

}
