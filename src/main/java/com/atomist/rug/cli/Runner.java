package com.atomist.rug.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import com.atomist.rug.cli.command.CommandInfoRegistry;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.cli.command.ReflectiveCommandRunner;
import com.atomist.rug.cli.command.utils.CommandHelpFormatter;
import com.atomist.rug.cli.output.Style;
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

        int returnCode = 0;
        
        if (args.length == 1 && (args[0].equals("-v") || args[0].equals("--version"))) {
            printVersion();
        }
        else {
            CommandLine commandLine = null;
            try {
                commandLine = CommandUtils.parseCommandline(args, registry);
                returnCode = runCommand(args, commandLine);
            }
            catch (Throwable e) {
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
        }
        System.exit(returnCode);
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

    private void printVersion() {
        VersionInformation versionInfo = VersionUtils.readVersionInformation();

        log.info(Style.bold(Constants.COMMAND + " " + versionInfo.version()));
        log.info(String.format("%s (git revision %s; last commit %s)", versionInfo.repo(),
                versionInfo.sha(), versionInfo.date()));
    }

    private int runCommand(String[] args, CommandLine commandLine) {
        if ((commandLine.hasOption('?') || commandLine.hasOption('h'))
                && commandLine.getArgList().isEmpty()) {
            printHelp();
        }
        else if (commandLine.getArgList().isEmpty()) {
            printHelp();
            return 1;
        }
        else if (commandLine.hasOption('?') || commandLine.hasOption('h')) {
            printCommandHelp(commandLine);
        }
        else if (commandLine.getArgList().size() >= 1) {
            return new ReflectiveCommandRunner(registry).runCommand(args, commandLine);
        }
        return 0;
    }
}
