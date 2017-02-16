package com.atomist.rug.cli;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.cli.command.CommandInfoRegistry;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.cli.command.ShellCommandRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.version.VersionThread;
import com.atomist.rug.cli.version.VersionUtils;
import com.atomist.rug.cli.version.VersionUtils.VersionInformation;

/**
 * Simple command runner that takes are of most basic command.
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
                commandLine = CommandUtils.parseCommandline(args, registry);
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
        if (commandLine != null && commandLine.hasOption('X')) {
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

        log.info(Style.bold(Constants.COMMAND + " " + versionInfo.version()));
        log.info(String.format("%s (git revision %s; last commit %s)", versionInfo.repo(),
                versionInfo.sha(), versionInfo.date()));
    }

    private int runCommand(String[] args, CommandLine commandLine) {
        if ((commandLine.hasOption('?') || commandLine.hasOption('h'))
                && commandLine.getArgList().isEmpty()) {
            args = new String[] { "help" };
            commandLine = CommandUtils.parseCommandline(args, registry);
            new ShellCommandRunner(registry).runCommand(args, commandLine);
        }
        else if (commandLine.getArgList().isEmpty()) {
            log.error("Missing command argument.\n" + "\n"
                    + "Run the following command for usage help:\n" + "  rug --help");
            return 1;
        }
        else if (commandLine.getArgList().size() >= 1) {
            return new ShellCommandRunner(registry).runCommand(args, commandLine);
        }
        return 0;
    }
}
