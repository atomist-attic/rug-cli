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
    public void testGestureReadingFromDirectory() {
        Map<String, Gesture> shortcuts = GestureReader
                .readFromDirectory(new File("src/main/resources/gestures"));
        assertEquals(4, shortcuts.size());
        assertTrue(shortcuts.containsKey("generate rug project"));
        assertTrue(shortcuts.containsKey("clone rug project"));
        assertEquals(13, shortcuts.get("generate rug project").commands().size());
        assertEquals(3, shortcuts.get("generate rug project").placeholders().size());
    }

    @Test
    public void testGestureReadingFromClasspath() {
        Map<String, Gesture> shortcuts = GestureReader.readFromClasspath();
        assertEquals(4, shortcuts.size());
        assertTrue(shortcuts.containsKey("generate rug project"));
        assertTrue(shortcuts.containsKey("clone rug project"));
        assertEquals(13, shortcuts.get("generate rug project").commands().size());
        assertEquals(3, shortcuts.get("generate rug project").placeholders().size());
    }

}
