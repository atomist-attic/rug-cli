package com.atomist.rug.cli.command.shell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;

/**
 * {@link Completer} for completion of operation names, like Editor and Generator names.
 */
public class OperationCompleter implements Completer {

    // TODO add commands for new handlers
    private static final List<String> COMMANDS = Arrays.asList("edit", "ed", "generate", "gen",
            "describe", "desc", "command", "trigger");

    private long timestamp = -1;

    private ReadContext ctx = null;

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        if (line.words().size() >= 1) {

            String command = line.words().get(0);

            if (COMMANDS.contains(command)) {

                init();

                switch (command) {
                case "edit":
                case "ed":
                    completeBasedOnJsonpathMatches("editors", line.words(), candidates);
                    break;
                case "generate":
                case "gen":
                    completeBasedOnJsonpathMatches("generators", line.words(), candidates);
                    break;
                case "command":
                    completeBasedOnJsonpathMatches("command_handlers", line.words(), candidates);
                    break;
                case "trigger":
                    completeBasedOnJsonpathMatches("event_handlers", line.words(), candidates);
                    break;
                case "describe":
                case "desc":
                    if (line.words().size() >= 2) {
                        String subCommand = line.words().get(1);
                        switch (subCommand) {
                        case "editor":
                            completeBasedOnJsonpathMatches("editors", line.words(), candidates);
                            break;
                        case "generator":
                            completeBasedOnJsonpathMatches("generators", line.words(), candidates);
                            break;
                        case "reviewer":
                            completeBasedOnJsonpathMatches("reviewers", line.words(), candidates);
                            break;
                        case "command-handler":
                            completeBasedOnJsonpathMatches("command_handlers", line.words(),
                                    candidates);
                            break;
                        case "event-handler":
                            completeBasedOnJsonpathMatches("event_handlers", line.words(),
                                    candidates);
                            break;
                        case "response-handler":
                            completeBasedOnJsonpathMatches("response_handlers", line.words(),
                                    candidates);
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

    private void completeBasedOnJsonpathMatches(String kind, List<String> words,
            List<Candidate> candidates) {
        if (ctx != null) {
            try {
                List<String> names = ctx.read(String.format("$.%s[*].name", kind));
                Optional<String> name = names.stream().filter(words::contains).map(String::toString)
                        .findFirst();
                if (name.isPresent()) {
                    List<String> parameterNames = ctx.read(String
                            .format("$.%s[?(@.name=='%s')].parameters[*].name", kind, name.get()));
                    parameterNames.stream()
                            .filter(p -> !kind.equals("generators")
                                    || (kind.equals("generators") && !p.equals("project_name")))
                            .filter(p -> words.stream().noneMatch(w -> w.startsWith(p + "=")))
                            .forEach(n -> candidates
                                    .add(new Candidate(n + "=", n, null, null, null, null, false)));
                }
                else {
                    names.forEach(n -> candidates.add(new Candidate(n)));
                }
            }
            catch (PathNotFoundException e) {
                // This is ok as it means we don't have the matching operation in the cache
            }
        }
    }

    private void init() {
        if (ShellUtils.SHELL_OPERATIONS.exists()
                && ShellUtils.SHELL_OPERATIONS.lastModified() > timestamp) {
            this.timestamp = ShellUtils.SHELL_OPERATIONS.lastModified();
            try {
                this.ctx = JsonPath.parse(FileUtils.readFileToString(ShellUtils.SHELL_OPERATIONS,
                        StandardCharsets.ISO_8859_1));

            }
            catch (IOException e) {
            }
        }
    }
}
