package com.atomist.rug.cli.command.gesture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;

public abstract class GestureReader {

    @SuppressWarnings("unchecked")
    public static Map<String, Gesture> readFromDirectory(File gestureDirectory) {
        Map<String, Gesture> gesture = new HashMap<>();
        if (gestureDirectory.exists()) {
            Yaml yaml = new Yaml();
            FileUtils.listFiles(gestureDirectory, new String[] { "yml" }, true).forEach(f -> {
                try {
                    Map<String, Object> s = (Map<String, Object>) yaml.load(new FileInputStream(f));
                    s.entrySet().stream().forEach(e -> {
                        Gesture g = readGesture(e);
                        gesture.put(g.name(), g);
                    });
                }
                catch (FileNotFoundException e) {
                    // Very unlikely to happen as we just asked to list the files
                }
            });
        }
        return gesture;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Gesture> readFromClasspath() {
        Map<String, Gesture> gesture = new HashMap<>();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
                Thread.currentThread().getContextClassLoader());
        try {
            Resource[] resources = resolver.getResources("classpath*:gestures/**/*.yml");

            Yaml yaml = new Yaml();
            Arrays.asList(resources).forEach(r -> {
                try {
                    Map<String, Object> s = (Map<String, Object>) yaml.load(r.getInputStream());
                    s.entrySet().stream().forEach(e -> {
                        Gesture g = readGesture(e);
                        gesture.put(g.name(), g);
                    });
                }
                catch (IOException e) {
                    // ignore
                }
            });
        }
        catch (IOException e) {
            // ignore
        }
        return gesture;
    }
    
    @SuppressWarnings("unchecked")
    private static Gesture readGesture(Map.Entry<String, Object> e) {
        String name = e.getKey();
        Map<String, Object> data = (Map<String, Object>) e.getValue();
        String usage = (String) data.get("usage");
        String description = (String) data.get("description");
        String detail = (String) data.get("detail");
        List<String> cmds = null;
        if (data.containsKey("commands")) {
            cmds = (List<String>) data.get("commands");
        }
        return new Gesture(name, cmds, usage, description, detail);
    }
}
