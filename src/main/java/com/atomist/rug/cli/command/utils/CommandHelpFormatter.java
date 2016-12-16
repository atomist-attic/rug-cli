package com.atomist.rug.cli.command.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.text.WordUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.CommandInfo;
import com.atomist.rug.cli.command.CommandInfoRegistry;

public class CommandHelpFormatter {

    public static String HELP_FOOTER = "\n\nPlease report issues at https://github.com/atomist/rug-cli";

    private static int WRAP = 70;

    public String createString(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, ' ');
        return "\n" + new String(chars);
    }

    public String printCommandHelp(CommandInfo description) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Usage: %s %s\n", Constants.COMMAND, description.usage()));
        sb.append(String.format("%s.\n", description.description()));

        printOptions(description.globalOptions(), sb, "Options");
        printOptions(description.options(), sb, "Command Options");

        sb.append("\n");
        sb.append(WordUtils.wrap(description.detail(), WRAP));
        sb.append(HELP_FOOTER);

        return sb.toString();

    }

    public String printHelp(CommandInfoRegistry registry, Options options) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Usage: %s [OPTION]... [COMMAND]...\n", Constants.COMMAND));
        sb.append("Work with Rugs like editors or generators.\n");

        printOptions(options, sb, "Options");

        sb.append("\n");

        printCommands(sb, registry);

        sb.append("\n");
        sb.append(String.format("Run '%s COMMAND --help' for more detailed information on COMMAND.",
                Constants.COMMAND));
        sb.append(HELP_FOOTER);

        return sb.toString();
    }

    private int getOptionLenght(Option opt) {
        if (opt.hasArgName()) {
            return (opt.getLongOpt() + " " + opt.getArgName()).length();
        }
        else {
            return opt.getLongOpt().length();
        }
    }

    private void printCommands(StringBuilder sb, CommandInfoRegistry registry) {
        sb.append("Available commands:\n");
        int length = registry.commands().stream()
                .max((o1, o2) -> Integer.compare(o1.name().length(), o2.name().length())).get()
                .name().length() + 1;
        String formatString = "  %-" + length + "s %s\n";
        registry.commands().forEach(c -> sb.append(String.format(formatString, c.name(),
                WordUtils.wrap(c.description(), WRAP - length, createString(length + 3), false))));
    }

    private void printOptions(Options options, StringBuilder sb, String label) {
        if (options.getOptions().size() == 0) {
            return;
        }

        sb.append("\n");

        int length = getOptionLenght(options.getOptions().stream()
                .max((o1, o2) -> Integer.compare(getOptionLenght(o1), getOptionLenght(o2))).get())
                + 6;
        String formatString = "  %-" + length + "s %s\n";

        sb.append(label).append(":\n");
        options.getOptions().stream().collect(Collectors.groupingBy(Option::getDescription))
                .entrySet().stream().sorted((o1, o2) -> o1.getValue().get(0).getOpt()
                        .compareTo(o2.getValue().get(0).getOpt()))
                .forEach(e -> {
                    Option opt = e.getValue().stream().findFirst().get();
                    String ops = e.getValue().stream().map(o -> "-" + o.getOpt())
                            .collect(Collectors.joining(",")) + ",--" + opt.getLongOpt();
                    if (opt.hasArgName()) {
                        ops += " " + opt.getArgName();
                    }
                    sb.append(String.format(formatString, ops, WordUtils.wrap(opt.getDescription(),
                            WRAP - length, createString(length + 3), false)));

                });
    }
}
