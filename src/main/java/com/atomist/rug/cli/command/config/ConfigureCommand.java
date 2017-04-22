package com.atomist.rug.cli.command.config;

import java.io.File;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.annotation.Validator;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.settings.SettingsWriter;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.StringUtils;

public class ConfigureCommand extends AbstractAnnotationBasedCommand {

    @Validator
    public void validate(Settings settings, CommandLine commandLine,
            @Argument(index = 1, defaultValue = "") String command) {
        switch (command) {
        case "default":
            if (commandLine.getArgList().size() > 2
                    && !"archive".equals(commandLine.getArgList().get(2))) {
                throw new CommandException(
                        "Invalid SUBCOMMAND provided. Please use either default archive or repositories.",
                        "configure");
            }
            break;
        case "repositories":
            if (!settings.getConfigValue(Settings.GIHUB_TOKEN_KEY, String.class).isPresent()) {
                throw new CommandException(
                        "No token configured. Please run the login command before running this command.",
                        "repositories configure");
            }
            break;
        default:
            throw new CommandException(
                    "Invalid SUBCOMMAND provided. Please use either default archive or repositories.",
                    "configure");
        }
    }

    @Command
    public void run(CommandLine commandLine, @Argument(index = 1, defaultValue = "") String command,
            @Option("archive-version") String version, @Option("global") boolean global,
            @Option("delete") boolean delete, @Option("save") boolean save, Settings settings) {

        switch (command) {
        case "default":
            if (commandLine.getArgList().size() > 2
                    && "archive".equals(commandLine.getArgList().get(2))) {
                if (delete) {
                    delete(global);
                }
                if (save) {
                    configure(commandLine, global, version);
                }
            }
            break;
        case "repositories":
            new ConfigureRepositoriesOperations().run(settings);
            break;
        default:
        }
    }

    private void configure(CommandLine commandLine, boolean global, String version) {
        String defaultGroup = null;
        String defaultArtifact = null;
        String defaultVersion = null;

        if (commandLine.getArgList().size() > 3) {
            String coordinates = commandLine.getArgList().get(3);
            if (coordinates != null) {
                String[] parts = coordinates.split(":");
                if (parts.length >= 1) {
                    defaultGroup = parts[0];
                }
                if (parts.length == 2) {
                    defaultArtifact = parts[1];
                }
                defaultVersion = version;
            }
        }
        else {
            throw new CommandException("No valid ARCHIVE identifier specified.", "default");
        }

        File settingsFile = settingsFile(global);
        Settings settings = null;
        if (settingsFile.exists()) {
            settings = SettingsReader.settingsFromFile(settingsFile);
        }
        else {
            settings = new Settings();
        }

        settings.getDefaults().setGroup(defaultGroup);
        settings.getDefaults().setArtifact(defaultArtifact);
        settings.getDefaults().setVersion(defaultVersion);

        SettingsWriter.write(settings, settingsFile);

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " "
                + Style.bold("Default (%s)", (global ? "global" : "project")));
        log.info("  group: %s", Style.yellow(defaultGroup));
        if (defaultArtifact != null) {
            log.info("  artifact: %s", Style.yellow(defaultArtifact));
        }
        if (defaultVersion != null) {
            log.info("  version: %s", Style.yellow(defaultVersion));
        }

        if (global) {
            log.newline();
            log.info(Style.green("Successfully configured global default archive configuration"));
        }
        else {
            log.newline();
            log.info(Style.green("Successfully configured project default archive configuration"));
        }
    }

    private void delete(boolean global) {

        File settingsFile = settingsFile(global);
        Settings settings = null;
        if (settingsFile.exists()) {
            settings = SettingsReader.settingsFromFile(settingsFile);
        }
        else {
            settings = new Settings();
        }
        settings.setDefaults(null);
        SettingsWriter.write(settings, settingsFile);

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " "
                + Style.bold("Default (%s)", (global ? "global" : "project")));
        log.info("  Deleted");
        if (global) {
            log.newline();
            log.info(Style.green("Successfully deleted global default archive"));
        }
        else {
            log.newline();
            log.info(Style.green("Successfully deleted project default archive"));
        }

    }

    private File settingsFile(boolean global) {
        File settingsFile = null;
        Optional<String> settingsFileOption = CommandLineOptions.getOptionValue("s");
        if (settingsFileOption.isPresent()) {
            settingsFile = new File(StringUtils.expandEnvironmentVars(settingsFileOption.get()));
        }
        else {
            if (global) {
                settingsFile = new File(SettingsReader.PATH);
            }
            else {
                File root = FileUtils.getUserDirectory();
                if (root != null) {
                    settingsFile = new File(root,
                            Constants.ATOMIST_ROOT + File.separator + Constants.CLI_CONFIG_NAME);
                    if (!settingsFile.exists()) {
                        settingsFile.getParentFile().mkdirs();
                    }
                }
            }
        }
        return settingsFile;
    }
}