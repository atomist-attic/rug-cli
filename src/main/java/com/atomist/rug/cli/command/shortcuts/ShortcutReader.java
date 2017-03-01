package com.atomist.rug.cli.command.shortcuts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

public abstract class ShortcutReader {

    @SuppressWarnings("unchecked")
    public static Map<String, Shortcut> read(File shortcutDirectory) {
        Map<String, Shortcut> shortcuts = new HashMap<>();
        if (shortcutDirectory.exists()) {
            Yaml yaml = new Yaml();
            FileUtils.listFiles(shortcutDirectory, new String[] { "yml" }, true).forEach(f -> {
                try {
                    Map<String, Object> s = (Map<String, Object>) yaml.load(new FileInputStream(f));
                    s.entrySet().stream().forEach(e -> {
                        Map<String, Object> data = (Map<String, Object>) e.getValue();
                        String name = e.getKey();
                        List<String> cmds = null;
                        if (data.containsKey("commands")) {
                            cmds = (List<String>) data.get("commands");
                            shortcuts.put(name, new Shortcut(name, cmds));
                        }
                    });
                }
                catch (FileNotFoundException e) {
                    // Very unlikely to happen as we just asked to list the files
                }
            });
        }

        return shortcuts;
    }

}
