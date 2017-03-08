package com.atomist.rug.cli.command.shell;

import java.io.IOException;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.kohsuke.github.GitHub;

import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.SettingsReader;

/**
 * {@link Completer} for owner= and repository= arguments.
 */
public class OwnerAndRepoCompleter implements Completer {

    private String token;

    public OwnerAndRepoCompleter() {
        this.token = SettingsReader.read().getConfigValue(Settings.GIHUB_TOKEN_KEY, String.class)
                .orElse(null);
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String word = line.word();
        if (word != null && word.startsWith("owner=")) {
            try {
                GitHub github = GitHub.connectUsingOAuth(token);
                github.getMyOrganizations().keySet().forEach(k -> {
                    candidates.add(new Candidate("owner=" + k, k, null, null, null, null, true));});
            }
            catch (IOException e) {
            }
        }
        else if (word != null && word.startsWith("repository=") || word.startsWith("repo=")) {
            
        }
    }
}
