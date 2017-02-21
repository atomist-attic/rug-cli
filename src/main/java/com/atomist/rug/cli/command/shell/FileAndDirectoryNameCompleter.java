package com.atomist.rug.cli.command.shell;

import com.atomist.rug.cli.Constants;

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.FileNameCompleter;

/**
 * Extension to JLine's {@link FileNameCompleter} that only completes on <code>-C</code> or
 * <code>--change-dir</code> arguments and <code>!</code> process executions .
 */
public class FileAndDirectoryNameCompleter extends FileNameCompleter {

    @Override
    public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
        if (commandLine.words().size() > 1) {
            String word = commandLine.words().get(commandLine.words().size() - 2);
            if ("-C".equals(word) || "--change-dir".equals(word)) {
                super.complete(reader, commandLine, candidates);
            }
            else if (commandLine.line().startsWith(Constants.SHELL_ESCAPE)) {
                super.complete(reader, commandLine, candidates);
            }
        }
    }
}
