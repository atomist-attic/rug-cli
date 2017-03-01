package com.atomist.rug.cli.command.shell;

import java.util.List;
import java.util.Optional;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.atomist.rug.cli.command.shortcuts.Shortcut;
import com.atomist.rug.cli.command.shortcuts.ShortcutRegistry;

/**
 * {@link Completer} for shortcuts.
 */
public class ShortcutCompleter implements Completer {

    private ShortcutRegistry registry;

    public ShortcutCompleter(ShortcutRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        List<String> words = line.words();
        if (words.size() == 1) {
            registry.shortcutNames()
                    .forEach(s -> candidates.add(new Candidate(s, s, null, null, null, s, true)));
        }
        else if (words.size() > 1) {
            String word = words.get(0);

            Optional<String> shortcut = registry.shortcutNames().stream()
                    .filter(s -> s.startsWith(word)).findFirst();
            if (shortcut.isPresent()) {
                Shortcut s = registry.findShortcut(shortcut.get()).get();

                s.placeholders().stream()
                        .filter(p -> !words.stream().filter(w -> w.startsWith(p + "=")).findAny()
                                .isPresent())
                        .forEach(c -> candidates
                                .add(new Candidate(c + "=", c, null, null, null, c, false)));
            }
        }
    }
}
