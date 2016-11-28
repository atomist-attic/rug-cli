package com.atomist.rug.cli.command.config;

import java.io.File;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;

import com.atomist.project.archive.DefaultAtomistConfig$;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.settings.SettingsWriter;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.StringUtils;

public class ConfigCommand extends AbstractAnnotationBasedCommand {

    private Log log = new Log(ConfigCommand.class);

    @Command
    public void run(CommandLine commandLine, @Option("archive-version") String version,
            @Option("global") boolean global, @Option("delete") boolean delete) {

        if (delete) {
            delete(global);
        }
        else {
            configure(commandLine, global, version);
        }
    }

    private void configure(CommandLine commandLine, boolean global, String version) {
        String defaultGroup = null;
        String defaultArtifact = null;
        String defaultVersion = null;

        if (commandLine.getArgList().size() > 1) {
            String coordinates = commandLine.getArgList().get(1);
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
            settings = new SettingsReader().settingsFromFile(settingsFile);
        }
        else {
            settings = new Settings();
        }

        settings.getDefaults().setGroup(defaultGroup);
        settings.getDefaults().setArtifact(defaultArtifact);
        settings.getDefaults().setVersion(defaultVersion);

        new SettingsWriter().write(settings, settingsFile);

        log.newline();
        log.info(Style.blue(Constants.DIVIDER) + " "
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
            settings = new SettingsReader().settingsFromFile(settingsFile);
        }
        else {
            settings = new Settings();
        }
        settings.setDefaults(null);
        new SettingsWriter().write(settings, settingsFile);

        log.newline();
        log.info(Style.blue(Constants.DIVIDER) + " "
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
                    settingsFile = new File(root, DefaultAtomistConfig$.MODULE$.atomistRoot()
                            + File.separator + "cli.yml");
                    if (!settingsFile.exists()) {
                        settingsFile.getParentFile().mkdirs();
                    }
                }
            }
        }
        return settingsFile;
    }
}