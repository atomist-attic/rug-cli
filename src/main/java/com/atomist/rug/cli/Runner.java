package com.atomist.rug.cli;

import com.atomist.rug.cli.command.CommandInfoRegistry;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.cli.command.ShellCommandRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.version.VersionThread;
import com.atomist.rug.cli.version.VersionUtils;
import com.atomist.rug.cli.version.VersionUtils.VersionInformation;

import org.apache.commons.cli.CommandLine;

/**
 * Simple command runner that parses the command line and reacts to help and version requests
 */
public class Runner {

    private final Log log = new Log(getClass());
    private final CommandInfoRegistry registry;
    private final VersionThread versionThread = new VersionThread();

    public Runner(CommandInfoRegistry registry) {
        this.registry = registry;
    }

    public void run(String[] args) {

        // Validate the JDK version
        VersionUtils.validateJdkVersion();

        int returnCode = 0;

        if (args.length == 1 && (args[0].equals("-v") || args[0].equals("--version"))) {
            printVersion();
        }
        else {
            CommandLine commandLine = null;
            try {
                commandLine = CommandUtils.parseInitialCommandline(args, registry);
                returnCode = runCommand(args, commandLine);
            }
            catch (ReloadException e) {
                throw e;
            }
            catch (Throwable e) {
                logException(commandLine, e);
                returnCode = 1;
            }

            printNewVersion();
        }
        System.exit(returnCode);

    }

    private void logException(CommandLine commandLine, Throwable e) {
        // Print stacktraces only if requested from the command line
        if (commandLine.hasOption("X")) {
            log.error(e);
        }
        else {
            log.error(e.getMessage());
        }
    }

    private void printNewVersion() {
        if (versionThread.getVersion().isPresent()) {
            log.info(Style.yellow("Newer version of rug %s is available",
                    versionThread.getVersion().get()));
        }
    }

    private void printVersion() {
        VersionInformation versionInfo = VersionUtils.readVersionInformation();

        log.info(Style.bold(Constants.command() + versionInfo.version()));
        log.info(String.format("%s (git revision %s; last commit %s)", versionInfo.repo(),
                versionInfo.sha(), versionInfo.date()));
    }

    private int runCommand(String[] args, CommandLine commandLine) {
        if ((commandLine.hasOption("?") || commandLine.hasOption("h"))
                && commandLine.getArgList().isEmpty()) {
            args = new String[] { "help" };
            new ShellCommandRunner(registry).runCommand("help", args);
        }
        else if (commandLine.getArgList().isEmpty()) {
            log.error("Missing command argument.\n" + "\n"
                    + "Run the following command for usage help:\n" + "  rug --help");
            return 1;
        }
        else {
            return new ShellCommandRunner(registry).runCommand(commandLine.getArgList().get(0),
                    args);
        }
        return 0;
    }
}
