package com.atomist.rug.cli.settings;

import java.io.File;
import java.io.IOException;

import com.atomist.rug.cli.utils.FileUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public abstract class SettingsWriter {

    public static void write(Settings settings, File file) {
        if (file != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        settingsToFile(settings, file);
    }

    private static void cleanSettings(Settings settings) {
        if (settings.getLocalRepository() != null
                && settings.getLocalRepository().getPath() == null) {
            settings.setLocalRepository(null);
        }
    }

    private static void settingsToFile(Settings settings, File settingsFile) {

        cleanSettings(settings);

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            mapper.writeValue(settingsFile, settings);
            FileUtils.setPermissionsToOwnerOnly(settingsFile);
        }
        catch (JsonParseException e) {
            throw new SettingsException(String.format("Error parsing configuration at '%s'",
                    settingsFile.getAbsolutePath().toString()), e);
        }
        catch (JsonMappingException e) {
            throw new SettingsException(String.format("Error mapping configuration at '%s'",
                    settingsFile.getAbsolutePath().toString()), e);
        }
        catch (IOException e) {
            throw new SettingsException(String.format("Cannot load configuration at '%s'",
                    settingsFile.getAbsolutePath().toString()), e);
        }
    }
}
