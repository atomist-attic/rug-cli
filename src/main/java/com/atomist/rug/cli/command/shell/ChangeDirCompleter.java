package com.atomist.rug.cli.command.shell;

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.FileNameCompleter;

public class ChangeDirCompleter extends FileNameCompleter {
    
    @Override
    public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
        if (commandLine.words().size() > 1) {
            String word = commandLine.words().get(commandLine.words().size() - 2);
            if ("-C".equals(word) || "--change-dir".equals(word)) {
                super.complete(reader, commandLine, candidates);
            }
        }
    }

}
