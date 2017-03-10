package com.atomist.rug.cli.command.gesture;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

import com.atomist.rug.cli.Constants;
import com.github.tomaslanger.chalk.Ansi;

public class Gesture {

    private static final PropertyPlaceholderHelper nonStrictHelper = new PropertyPlaceholderHelper(
            "${", "}", ":", true);
    private static final String PLACEHOLDER_PATTERN = "\\$\\{([.a-zA-Z_-]+)[.:a-zA-Z-_ \"]*\\}";

    private static final Properties STYLE_PLACEHOLDERS;
    static {
        STYLE_PLACEHOLDERS = new Properties();
        STYLE_PLACEHOLDERS.put("underline", Ansi.Modifier.UNDERLINE.getStart());
        STYLE_PLACEHOLDERS.put("/underline", Ansi.Modifier.UNDERLINE.getEnd());
        STYLE_PLACEHOLDERS.put("bold", Ansi.Modifier.BOLD.getStart());
        STYLE_PLACEHOLDERS.put("/bold", Ansi.Modifier.BOLD.getEnd());
        STYLE_PLACEHOLDERS.put("cyan", Ansi.Color.CYAN.getStart());
        STYLE_PLACEHOLDERS.put("/cyan", Ansi.Color.CYAN.getEnd());
        STYLE_PLACEHOLDERS.put("magenta", Ansi.Color.MAGENTA.getStart());
        STYLE_PLACEHOLDERS.put("/magenta", Ansi.Color.MAGENTA.getEnd());
        STYLE_PLACEHOLDERS.put("yellow", Ansi.Color.YELLOW.getStart());
        STYLE_PLACEHOLDERS.put("/yellow", Ansi.Color.YELLOW.getEnd());
        STYLE_PLACEHOLDERS.put("divider", Constants.DIVIDER);
        STYLE_PLACEHOLDERS.put("dot", Constants.DOT);
        STYLE_PLACEHOLDERS.put("seperator", Constants.SEPERATOR);
        STYLE_PLACEHOLDERS.put("line",
                Constants.SEPERATOR + Constants.SEPERATOR + Constants.SEPERATOR);
    }

    private final String name;

    private final List<String> commands;

    private final String usage;
    private final String description;
    private final String detail;

    public Gesture(String name, List<String> commands, String usage, String description,
            String detail) {
        this.name = name;
        this.commands = commands;
        this.usage = usage;
        this.description = description;
        this.detail = detail;
    }

    public String name() {
        return name;
    }

    public List<String> commands() {
        return commands;
    }

    public String usage() {
        return usage;
    }

    public String description() {
        return description;
    }

    public String detail() {
        return detail;
    }

    public Set<String> placeholders() {
        Pattern placeHolderPattern = Pattern.compile(PLACEHOLDER_PATTERN);
        Set<String> placeholders = new HashSet<>();
        commands.forEach(c -> {
            Matcher matcher = placeHolderPattern.matcher(c);
            while (matcher.find()) {
                if (!STYLE_PLACEHOLDERS.containsKey(matcher.group(1))) {
                    placeholders.add(matcher.group(1));
                }
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
            if (STYLE_PLACEHOLDERS.containsKey(placeholderName)) {
                return STYLE_PLACEHOLDERS.getProperty(placeholderName);
            }
            else {
                Optional<String> valueOptional = arguments.stream()
                        .filter(a -> a.startsWith(placeholderName + "=")).findAny();
                if (valueOptional.isPresent()) {
                    String value = valueOptional.get();
                    int ix = value.indexOf('=') + 1;
                    return value.substring(ix);
                }
            }
            return null;
        }
    }
}
