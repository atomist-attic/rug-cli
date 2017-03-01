package com.atomist.rug.cli.command.shortcuts;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;

import org.junit.Test;

public class ShortcutReaderTest {

    @Test
    public void testShortcutReading() {
        Map<String, Shortcut> shortcuts = ShortcutReader.read(new File("src/test/resources/shortcuts"));
        assertEquals(2, shortcuts.size());
        assertTrue(shortcuts.containsKey("init-rug-archive"));
        assertTrue(shortcuts.containsKey("clone-rug-archive"));
        assertEquals(3, shortcuts.get("init-rug-archive").commands().size());
        shortcuts.get("init-rug-archive").placeholders();
    }

}
