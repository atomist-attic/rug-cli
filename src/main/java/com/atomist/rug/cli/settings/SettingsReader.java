package com.atomist.rug.cli.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import com.atomist.project.archive.DefaultAtomistConfig$;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.settings.Settings.Authentication;
import com.atomist.rug.cli.settings.Settings.RemoteRepository;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;

public class SettingsReader {

    public static final String PATH = System.getProperty("user.home") + File.separator
            + DefaultAtomistConfig$.MODULE$.atomistRoot() + File.separator + "cli.yml";

    private static Log log = new Log(SettingsReader.class);

    public Settings read() {
        File settingsFile = new File(PATH);
        if (!CommandLineOptions.hasOption("s") && !settingsFile.exists()) {
            createDefaultSettingsFile(settingsFile);
        }
        else if (CommandLineOptions.hasOption("s")) {
            settingsFile = new File(StringUtils
                    .expandEnvironmentVars(CommandLineOptions.getOptionValue("s").get()));
        }

        Settings settings = settingsFromFile(settingsFile);
        Settings projectSettings = null;
        File userDir = org.apache.commons.io.FileUtils.getUserDirectory();
        if (userDir != null && userDir.exists()) {
            File projetSettingsFile = new File(userDir, ".atomist/cli.yml");
            if (projetSettingsFile.exists()) {
                projectSettings = settingsFromFile(projetSettingsFile);
                // now merge both files
                settings.override(projectSettings);
            }
        }
        return settings;
    }

    @SuppressWarnings("unchecked")
    public Settings settingsFromFile(File settingsFile) {
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

            return settings;
        }
        catch (FileNotFoundException e) {
            throw new SettingsException(String.format("Error parsing configuration at '%s'",
                    settingsFile.getAbsolutePath().toString()), e);
        }
    }

    private void createDefaultSettingsFile(File settingsFile) {
        try {
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("cli.yml"),
                    new FileOutputStream(settingsFile));
            FileUtils.setPermissionsToOwnerOnly(settingsFile);
            log.info("Created default configuration file at %s", settingsFile.getAbsolutePath());
        }
        catch (IOException e) {
            // worse thing that can happen here is that we inform the user later that no default
            // settings file could be found.
        }
    }
}