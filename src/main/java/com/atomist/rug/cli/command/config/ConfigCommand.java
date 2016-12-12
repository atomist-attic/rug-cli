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
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.settings.SettingsWriter;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.StringUtils;

public class ConfigCommand extends AbstractAnnotationBasedCommand {

	@Command
	public void run(CommandLine commandLine, @Argument(index = 1) String command,
			@Option("archive-version") String version, @Option("global") boolean global,
			@Option("delete") boolean delete) {
		
		switch (command) {
			case "delete":
				delete(global);
				break;
			case "save": 
				configure(commandLine, global, version);
				break;
			default:
				throw new CommandException("No or invalid ACTION provided.", "default");
		}
	}

	private void configure(CommandLine commandLine, boolean global, String version) {
		String defaultGroup = null;
		String defaultArtifact = null;
		String defaultVersion = null;

		if (commandLine.getArgList().size() > 2) {
			String coordinates = commandLine.getArgList().get(2);
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
		} else {
			throw new CommandException("No valid ARCHIVE identifier specified.", "default");
		}

		File settingsFile = settingsFile(global);
		Settings settings = null;
		if (settingsFile.exists()) {
			settings = new SettingsReader().settingsFromFile(settingsFile);
		} else {
			settings = new Settings();
		}

		settings.getDefaults().setGroup(defaultGroup);
		settings.getDefaults().setArtifact(defaultArtifact);
		settings.getDefaults().setVersion(defaultVersion);

		new SettingsWriter().write(settings, settingsFile);

		setResultView("default-save");
		addResultContext("global", global);
		addResultContext("group", defaultGroup);
		addResultContext("artifact", defaultArtifact);
		addResultContext("version", defaultVersion);
	}

	private void delete(boolean global) {

		File settingsFile = settingsFile(global);
		Settings settings = null;
		if (settingsFile.exists()) {
			settings = new SettingsReader().settingsFromFile(settingsFile);
		} else {
			settings = new Settings();
		}
		settings.setDefaults(null);
		new SettingsWriter().write(settings, settingsFile);
		
		setResultView("default-delete");
		addResultContext("global", global);
	}

	private File settingsFile(boolean global) {
		File settingsFile = null;
		Optional<String> settingsFileOption = CommandLineOptions.getOptionValue("s");
		if (settingsFileOption.isPresent()) {
			settingsFile = new File(StringUtils.expandEnvironmentVars(settingsFileOption.get()));
		} else {
			if (global) {
				settingsFile = new File(SettingsReader.PATH);
			} else {
				File root = FileUtils.getUserDirectory();
				if (root != null) {
					settingsFile = new File(root, Constants.ATOMIST_ROOT + File.separator + Constants.CLI_CONFIG_NAME);
					if (!settingsFile.exists()) {
						settingsFile.getParentFile().mkdirs();
					}
				}
			}
		}
		return settingsFile;
	}
}