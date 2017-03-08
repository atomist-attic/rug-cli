package com.atomist.rug.cli.command.gesture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

public abstract class GestureReader {

    @SuppressWarnings("unchecked")
    public static Map<String, Gesture> read(File gestureDirectory) {
        Map<String, Gesture> gesture = new HashMap<>();
        if (gestureDirectory.exists()) {
            Yaml yaml = new Yaml();
            FileUtils.listFiles(gestureDirectory, new String[] { "yml" }, true).forEach(f -> {
                try {
                    Map<String, Object> s = (Map<String, Object>) yaml.load(new FileInputStream(f));
                    s.entrySet().stream().forEach(e -> {
                        Map<String, Object> data = (Map<String, Object>) e.getValue();
                        String name = e.getKey();
                        List<String> cmds = null;
                        if (data.containsKey("commands")) {
                            cmds = (List<String>) data.get("commands");
                            gesture.put(name, new Gesture(name, cmds));
                        }
                    });
                }
                catch (FileNotFoundException e) {
                    // Very unlikely to happen as we just asked to list the files
                }
            });
        }

        return gesture;
    }

}
