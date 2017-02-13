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

public class CommandInfoCompleter implements Completer {

    private CommandInfoRegistry registry;

    public CommandInfoCompleter(CommandInfoRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        List<String> words = line.words();
        if (words.size() == 1) {
            registry.commands().forEach(c -> candidates.add(new Candidate(c.name())));
        }
        else if (words.size() > 1) {
            String word = words.get(0);
            if ("describe".equals(word)) {
                candidates.add(new Candidate("archive"));
                candidates.add(new Candidate("editor"));
                candidates.add(new Candidate("generator"));
                candidates.add(new Candidate("executor"));
                candidates.add(new Candidate("reviewer"));
            }

            Optional<CommandInfo> info = registry.commands().stream()
                    .filter(c -> c.name().equals(word)).findFirst();
            if (info.isPresent()) {
                completeOptions(info.get().globalOptions().getOptions(), candidates, words);
                completeOptions(info.get().options().getOptions(), candidates, words);
            }
        }
    }

    private void completeOptions(Collection<Option> options, List<Candidate> candidates,
            List<String> words) {
        // public Candidate(String value, String displ, String group, String descr, String suffix,
        // String key, boolean complete) {
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
                candidates.add(new Candidate("--" + o.getLongOpt(), "--" + o.getLongOpt(),
                        "options", null, null, o.toString(), true));
            }
            if (o.getOpt() != null) {
                candidates.add(new Candidate("-" + o.getOpt(), "-" + o.getOpt(), "options", null,
                        null, o.toString(), true));
            }
        });
    }

}
