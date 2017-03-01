package com.atomist.rug.cli.command.shortcuts;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.SystemUtils;

public class ShortcutRegistry {

    private static Map<String, Shortcut> shortcuts;

    public ShortcutRegistry() {
        init();
    }

    private void init() {
        if (shortcuts == null) {
            shortcuts = ShortcutReader.read(
                    new File(SystemUtils.getUserHome(), ".atomist" + File.separator + "shortcuts"));
        }
    }

    public Optional<Shortcut> findShortcut(String name) {
        return Optional.ofNullable(shortcuts.get(name));
    }
    
    public Collection<String> shortcutNames() {
        return shortcuts.keySet();
    }
    
    public Collection<Shortcut> shortcuts() {
        return shortcuts.values();
    }
}
