package com.atomist.rug.cli;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.CommandHelpFormatter;
import com.atomist.rug.cli.command.CommandInfoRegistry;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.cli.command.ReflectiveCommandRunner;
import com.atomist.rug.cli.command.utils.ParseExceptionProcessor;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.Timing;
import com.atomist.rug.cli.version.VersionThread;
import com.atomist.rug.cli.version.VersionUtils;
import com.atomist.rug.cli.version.VersionUtils.VersionInformation;

public class Runner {

    private final Log log = new Log(getClass());
    private final CommandInfoRegistry registry;
    private final VersionThread versionThread = new VersionThread();

    public Runner(CommandInfoRegistry registry) {
        this.registry = registry;
    }

    public void run(String[] args) throws ParseException {

        Timing timing = new Timing();

        if (args.length == 1 && (args[0].equals("-v") || args[0].equals("--version"))) {
            printVersion();
        }
        else {
            CommandLine commandLine = null;
            try {
                commandLine = parseCommandline(args);
                runCommand(args, commandLine);
            }
            catch (Throwable e) {
                // Extract root exception; cycle through nested exceptions to extract root cause
                e = extractRootCause(e);

                // Print stacktraces only if requested from the command line
                log.newline();
                if (commandLine != null && commandLine.hasOption('X')) {
                    log.error(e);
                }
                else {
                    log.error(e.getMessage());
                }
                System.exit(1);
            }

            if (versionThread.getVersion().isPresent()) {
                printNewVersion(versionThread.getVersion().get());
            }

            if (commandLine != null && commandLine.hasOption('t')) {
                printTimer(timing);
            }
        }
        System.exit(0);
    }

    private CommandLine parseCommandline(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(registry.allOptions(), args);
            CommandLineOptions.set(commandLine);
            return commandLine;
        }
        catch (ParseException e) {
            throw new CommandException(ParseExceptionProcessor.process(e));
        }
    }

    private void printCommandHelp(CommandLine commandLine) {
        log.info(new CommandHelpFormatter().printCommandHelp(registry.findCommand(commandLine)));
    }

    private void printHelp() {
        log.info(new CommandHelpFormatter().printHelp(registry, CommandUtils.options()));
    }

    private void printNewVersion(String version) {
        log.info(Style.yellow("Newer version of rug %s is available", version));
    }

    private void printTimer(Timing timing) {
        log.info("Command completed in " + timing.duration() + "s");
    }

    private void printVersion() {
        VersionInformation versionInfo = VersionUtils.readVersionInformation();

        log.info(Constants.COMMAND + " " + versionInfo.version());
        log.info(String.format("%s (git revision %s; last commit %s)", versionInfo.repo(),
                versionInfo.sha(), versionInfo.date()));
    }

    private void runCommand(String[] args, CommandLine commandLine) {
        if ((commandLine.hasOption('?') || commandLine.hasOption('h'))
                && commandLine.getArgList().isEmpty()) {
            printHelp();
        }
        else if (commandLine.getArgList().isEmpty()) {
            printHelp();
            System.exit(1);
        }
        else if (commandLine.hasOption('?') || commandLine.hasOption('h')) {
            printCommandHelp(commandLine);
        }
        else if (commandLine.getArgList().size() >= 1) {
            new ReflectiveCommandRunner(registry).runCommand(args, commandLine);
        }
    }

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
