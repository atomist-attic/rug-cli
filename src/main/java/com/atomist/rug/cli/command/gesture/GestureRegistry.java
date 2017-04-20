package com.atomist.rug.cli.command.gesture;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.SystemUtils;

public class GestureRegistry {

    private static Map<String, Gesture> gestures;

    public GestureRegistry() {
        init();
    }

    private void init() {
        if (gestures == null) {
            gestures = new HashMap<>();
            gestures.putAll(GestureReader.readFromClasspath());
            gestures.putAll(GestureReader.readFromDirectory(
                    new File(SystemUtils.getUserHome(), ".atomist" + File.separator + "gestures")));
        }
    }

    public Optional<Gesture> findGesture(String[] args) {
        String name = null;
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("-")) {
                if (name == null) {
                    name = args[i];
                }
                else {
                    name = name + " " + args[i];
                }

                if (gestures.containsKey(name)) {
                    return Optional.of(gestures.get(name));
                }
            }
        }
        return Optional.empty();
    }

    public Collection<String> gestureNames() {
        return gestures.keySet();
    }

    public Collection<Gesture> gestures() {
        return gestures.values();
    }
}
