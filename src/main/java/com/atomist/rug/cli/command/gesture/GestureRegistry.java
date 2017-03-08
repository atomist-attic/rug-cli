package com.atomist.rug.cli.command.gesture;

import java.io.File;
import java.util.Collection;
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
            gestures = GestureReader.read(
                    new File(SystemUtils.getUserHome(), ".atomist" + File.separator + "gestures"));
        }
    }

    public Optional<Gesture> findGesture(String name) {
        return Optional.ofNullable(gestures.get(name));
    }

    public Collection<String> gestureNames() {
        return gestures.keySet();
    }

    public Collection<Gesture> gestures() {
        return gestures.values();
    }
}
