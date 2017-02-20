package com.atomist.rug.cli.command.shell;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.cli.Option;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.atomist.rug.cli.command.CommandInfo;
import com.atomist.rug.cli.command.CommandInfoRegistry;

/**
 * {@link Completer} for command, sub-command as well as options.
 */
public class CommandInfoCompleter implements Completer {

    private CommandInfoRegistry registry;

    public CommandInfoCompleter(CommandInfoRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        List<String> words = line.words();
        if (words.size() == 1) {
            registry.commands().forEach(c -> {
                candidates.add(new Candidate(c.name(), c.name(), null, null, null, c.name(), true));
                c.aliases().forEach(
                        a -> candidates.add(new Candidate(a, a, null, null, null, c.name(), true)));
            });
            candidates.add(new Candidate("exit", "exit", null, null, null, "q", true));
            candidates.add(new Candidate("quit", "quit", null, null, null, "q", true));
            candidates.add(new Candidate("q", "q", null, null, null, "q", true));
            candidates.add(new Candidate("clear", "clear", null, null, null, "cls", true));
            candidates.add(new Candidate("cls", "cls", null, null, null, "cls", true));
        }
        else if (words.size() > 1) {
            String word = words.get(0);

            Optional<CommandInfo> info = registry.commands().stream()
                    .filter(c -> c.name().equals(word) || c.aliases().contains(word)).findFirst();
            if (info.isPresent()) {

                // only complete subCommand if none exists already
                if (words.size() <= 2
                        || (words.size() > 2 && !info.get().subCommands().contains(words.get(1)))) {
                    info.get().subCommands().stream()
                            .forEach(s -> candidates.add(new Candidate(s)));
                }

                completeOptions(info.get().globalOptions().getOptions(), candidates, words);
                completeOptions(info.get().options().getOptions(), candidates, words);
            }
        }
    }

    private void completeOptions(Collection<Option> options, List<Candidate> candidates,
            List<String> words) {
        options.stream().filter(o -> {
            if (o.hasLongOpt() && words.contains("--" + o.getLongOpt())) {
                return false;
            }
            if (o.getOpt() != null && words.contains("-" + o.getOpt())) {
                return false;
            }
            return true;
        }).forEach(o -> {
            if (o.hasLongOpt()) {
                candidates.add(new Candidate("--" + o.getLongOpt(), "--" + o.getLongOpt(), null,
                        null, null, o.toString(), true));
            }
            if (o.getOpt() != null) {
                candidates.add(new Candidate("-" + o.getOpt(), "-" + o.getOpt(), null, null, null,
                        o.toString(), true));
            }
        });
    }

}
