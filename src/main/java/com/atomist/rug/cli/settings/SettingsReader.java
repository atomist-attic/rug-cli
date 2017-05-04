package com.atomist.rug.cli.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.settings.Settings.Authentication;
import com.atomist.rug.cli.settings.Settings.RemoteRepository;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;

public abstract class SettingsReader {

    public static final String PATH = org.apache.commons.io.FileUtils.getUserDirectoryPath()
            + File.separator + Constants.ATOMIST_ROOT + File.separator + Constants.CLI_CONFIG_NAME;

    private static Log log = new Log(SettingsReader.class);

    public static Settings read() {
        File settingsFile = new File(PATH);
        if (!CommandLineOptions.hasOption("settings") && !settingsFile.exists()) {
            createDefaultSettingsFile(settingsFile);
        }
        else if (CommandLineOptions.hasOption("settings")) {
            settingsFile = new File(StringUtils
                    .expandEnvironmentVars(CommandLineOptions.getOptionValue("settings").get()));
        }

        Settings settings = settingsFromFile(settingsFile);
        readProjectSettings(settings);
        return settings;
    }

    @SuppressWarnings("unchecked")
    public static Settings settingsFromFile(File settingsFile) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> data = (Map<String, Object>) yaml
                    .load(new FileInputStream(settingsFile));

            Settings settings = new Settings();
            
            
            // For the next releases we keep the old '-' keys around for reading
            if (data.containsKey("local-repository")
                    && ((Map<String, Object>) data.get("local-repository")).containsKey("path")) {
                settings.getLocalRepository().setPath(
                        (String) ((Map<String, Object>) data.get("local-repository")).get("path"));
            }
            else if (data.containsKey("local_repository")
                    && ((Map<String, Object>) data.get("local_repository")).containsKey("path")) {
                settings.getLocalRepository().setPath(
                        (String) ((Map<String, Object>) data.get("local_repository")).get("path"));
            }

            if (data.containsKey("remote-repositories")
                    || data.containsKey("remote_repositories")) {
                Map<String, Map<String, Object>> repos = (Map<String, Map<String, Object>>) data
                        .get("remote-repositories");
                if (repos == null) {
                    repos = (Map<String, Map<String, Object>>) data.get("remote_repositories");
                }
                repos.entrySet().forEach(r -> {
                    Map<String, Object> repo = r.getValue();
                    boolean publish = (Boolean) repo.get("publish");
                    String url = (String) repo.get("url");
                    String name = (String) repo.get("name");
                    RemoteRepository rr = new RemoteRepository();
                    rr.setUrl(url);
                    rr.setName(name);
                    rr.setPublish(publish);

                    Map<String, Object> auth = (Map<String, Object>) repo.get("authentication");
                    if (auth != null) {
                        Authentication authentication = new Authentication();
                        authentication.setUsername((String) auth.get("username"));
                        authentication.setPassword((String) auth.get("password"));
                        rr.setAuthentication(authentication);
                    }

                    settings.getRemoteRepositories().put(r.getKey(), rr);
                });

            }

            if (data.containsKey("default")) {
                Map<String, Object> defaults = (Map<String, Object>) data.get("default");
                settings.getDefaults().setGroup((String) defaults.get("group"));
                settings.getDefaults().setArtifact((String) defaults.get("artifact"));
                settings.getDefaults().setVersion((String) defaults.get("version"));
            }

            if (data.containsKey("configuration")) {
                Map<String, Object> config = (Map<String, Object>) data.get("configuration");
                settings.setConfig(config);
            }

            return settings;
        }
        catch (FileNotFoundException e) {
            throw new SettingsException(String.format("Error parsing configuration at '%s'",
                    settingsFile.getAbsolutePath().toString()), e);
        }
    }

    private static void createDefaultSettingsFile(File settingsFile) {
        try {
            if (!settingsFile.getParentFile().exists()) {
                settingsFile.getParentFile().mkdirs();
            }
            IOUtils.copy(SettingsReader.class.getClassLoader().getResourceAsStream(
                    Constants.CLI_CONFIG_NAME), new FileOutputStream(settingsFile));
            FileUtils.setPermissionsToOwnerOnly(settingsFile);
            log.info("Created default configuration file at %s", settingsFile.getAbsolutePath());
        }
        catch (IOException e) {
            // worse thing that can happen here is that we inform the user later that no default
            // settings file could be found.
        }
    }

    private static void readProjectSettings(Settings settings) {
        if (!CommandLineOptions.hasOption("settings")) {
            Settings projectSettings = null;
            Optional<File> userDir = FileUtils.getWorkingDirectory();
            if (userDir.isPresent() && userDir.get().exists()) {
                File projetSettingsFile = new File(userDir.get(),
                        Constants.ATOMIST_ROOT + File.separator + Constants.CLI_CONFIG_NAME);
                if (projetSettingsFile.exists()) {
                    projectSettings = settingsFromFile(projetSettingsFile);
                    // now merge both files
                    settings.override(projectSettings);
                }
            }
        }
    }
}
