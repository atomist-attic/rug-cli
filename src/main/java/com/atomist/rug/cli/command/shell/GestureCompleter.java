package com.atomist.rug.cli.command.shell;

import java.util.List;
import java.util.Optional;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.atomist.rug.cli.command.gesture.Gesture;
import com.atomist.rug.cli.command.gesture.GestureRegistry;

/**
 * {@link Completer} for shortcuts.
 */
public class GestureCompleter implements Completer {

    private GestureRegistry registry;

    public GestureCompleter(GestureRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        List<String> words = line.words();
        if (words.size() == 1) {
            registry.gestureNames()
                    .forEach(s -> candidates.add(new Candidate(s, s, null, null, null, s, true)));
        }
        else if (words.size() > 1) {
            String word = words.get(0);

            Optional<String> shortcut = registry.gestureNames().stream()
                    .filter(s -> s.startsWith(word)).findFirst();
            if (shortcut.isPresent()) {
                Gesture s = registry.findGesture(shortcut.get()).get();

                s.placeholders().stream()
                        .filter(p -> !words.stream().filter(w -> w.startsWith(p + "=")).findAny()
                                .isPresent())
                        .forEach(c -> candidates
                                .add(new Candidate(c + "=", c, null, null, null, c, false)));
            }
        }
    }
}
