package com.atomist.rug.cli.command.shell;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/**
 * {@link Completer} for Rug archive coordinates 
 */
public class ArchiveNameCompleter implements Completer {

    private static final List<String> COMMANDS = Arrays.asList("load", "shell", "repl", "sh");

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        if (line.words().size() == 2) {
            String cmd = line.words().get(0);
            String word = line.word();
            
            if (COMMANDS.contains(cmd)) {
                // group is already specified; only provide artifacts now
                if (word.contains(":")) {
                    String group = word.split(":")[0] + ":";
                    archivesFromCache().stream().filter(a -> a.startsWith(group))
                            .collect(Collectors.toSet()).forEach(a -> candidates
                                    .add(new Candidate(a)));
                }
                // sill completing group
                else {
                    archivesFromCache().stream().map(a -> a.split(":")[0])
                            .collect(Collectors.toSet()).forEach(a -> candidates
                                    .add(new Candidate(a + ":", a, null, null, null, null, false)));
                }
            }
        }
    }

    private List<String> archivesFromCache() {
        List<String> archives = new ArrayList<>();
        if (ShellUtils.SHELL_ARCHIVES.exists()) {
            try {
                IOUtils.lineIterator(new FileInputStream(ShellUtils.SHELL_ARCHIVES),
                        StandardCharsets.UTF_8).forEachRemaining((l) -> archives.add(l));
            }
            catch (IOException e) {
            }
        }
        return archives;
    }
    
    // private List<String> archivesFromRepository() {
    // List<String> archives = new ArrayList<>();
    //
    // }

}
