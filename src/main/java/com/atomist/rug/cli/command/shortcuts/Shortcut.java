package com.atomist.rug.cli.command.shortcuts;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

public class Shortcut {

    private static final PropertyPlaceholderHelper nonStrictHelper = new PropertyPlaceholderHelper(
            "${", "}", ":", true);

    private static final String PLACEHOLDER_PATTERN = "\\$\\{([.a-zA-Z_-]+)[.:a-zA-Z-_]*\\}";
    
    private final String name;

    private final List<String> commands;

    public Shortcut(String name, List<String> commands) {
        this.name = name;
        this.commands = commands;
    }

    public String name() {
        return name;
    }

    public List<String> commands() {
        return commands;
    }
    
    public Set<String> placeholders() {
        Pattern pattern = Pattern.compile(PLACEHOLDER_PATTERN);
        Set<String> placeholders = new HashSet<>();
        commands.forEach(c -> {
            Matcher matcher = pattern.matcher(c);
            while (matcher.find()) {
                placeholders.add(matcher.group(1));
            }
        });
        return placeholders;
    }

    public String toCommand(CommandLine commandLine) {
        String command = StringUtils.collectionToDelimitedString(commands, " && ");
        return nonStrictHelper.replacePlaceholders(command,
                new CommandLinePlaceholderResolver(commandLine.getArgList()));
    }

    private static class CommandLinePlaceholderResolver
            implements PropertyPlaceholderHelper.PlaceholderResolver {

        private final List<String> arguments;

        public CommandLinePlaceholderResolver(List<String> arguments) {
            this.arguments = arguments;
        }

        @Override
        public String resolvePlaceholder(String placeholderName) {
            Optional<String> valueOptional = arguments.stream()
                    .filter(a -> a.startsWith(placeholderName + "=")).findAny();
            if (valueOptional.isPresent()) {
                String value = valueOptional.get();
                int ix = value.indexOf('=') + 1;
                return value.substring(ix);
            }
            else {
                return null;
            }
        }
    }
}
