package com.atomist.rug.cli.command.gesture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import com.atomist.rug.cli.command.gesture.Gesture;
import com.atomist.rug.cli.command.gesture.GestureReader;

public class GestureReaderTest {

    @Test
    public void testShortcutReading() {
        Map<String, Gesture> shortcuts = GestureReader
                .read(new File("src/test/resources/gestures"));
        assertEquals(2, shortcuts.size());
        assertTrue(shortcuts.containsKey("init-rug-archive"));
        assertTrue(shortcuts.containsKey("clone-rug-archive"));
        assertEquals(10, shortcuts.get("init-rug-archive").commands().size());
        shortcuts.get("init-rug-archive").placeholders();
    }

}
