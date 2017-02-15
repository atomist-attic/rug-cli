package com.atomist.rug.cli.settings;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.settings.Settings.Authentication;
import com.atomist.rug.cli.settings.Settings.RemoteRepository;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class SettingsReader {

    public static final String PATH = org.apache.commons.io.FileUtils.getUserDirectoryPath()
            + File.separator + Constants.ATOMIST_ROOT + File.separator + Constants.CLI_CONFIG_NAME;

    private static Log log = new Log(SettingsReader.class);

    public static Settings read() {
        File settingsFile = new File(PATH);
        if (!CommandLineOptions.hasOption("s") && !settingsFile.exists()) {
            createDefaultSettingsFile(settingsFile);
        }
        else if (CommandLineOptions.hasOption("s")) {
            settingsFile = new File(StringUtils
                    .expandEnvironmentVars(CommandLineOptions.getOptionValue("s").get()));
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

            if (data.containsKey("local-repository")
                    && ((Map<String, Object>) data.get("local-repository")).containsKey("path")) {
                settings.getLocalRepository().setPath(
                        (String) ((Map<String, Object>) data.get("local-repository")).get("path"));
            }

            if (data.containsKey("remote-repositories")) {
                Map<String, Map<String, Object>> repos = (Map<String, Map<String, Object>>) data
                        .get("remote-repositories");
                repos.entrySet().forEach(r -> {
                    Map<String, Object> repo = r.getValue();
                    boolean publish = (Boolean) repo.get("publish");
                    String url = (String) repo.get("url");

                    RemoteRepository rr = new RemoteRepository();
                    rr.setUrl(url);
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

            if (data.containsKey("catalogs")) {
                List<String> urls = (List<String>) data.get("catalogs");
                urls.forEach(u -> settings.getCatalogs().addUrl(u));
            }
            
            if (data.containsKey("token")) {
                String token = (String) data.get("token");
                settings.setToken(token);
            }
            
            if (data.containsKey("config")) {
                Map<String, Object> config = (Map<String, Object>) data.get("config");
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
            IOUtils.copy(SettingsReader.class.getClassLoader().getResourceAsStream(Constants.CLI_CONFIG_NAME),
                    new FileOutputStream(settingsFile));
            FileUtils.setPermissionsToOwnerOnly(settingsFile);
            log.info("Created default configuration file at %s", settingsFile.getAbsolutePath());
        }
        catch (IOException e) {
            // worse thing that can happen here is that we inform the user later that no default
            // settings file could be found.
        }
    }

    private static void readProjectSettings(Settings settings) {
        if (!CommandLineOptions.hasOption("s")) {
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
