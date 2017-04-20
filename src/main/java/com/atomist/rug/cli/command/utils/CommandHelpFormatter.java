package com.atomist.rug.cli.command.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.text.WordUtils;
import org.springframework.util.StringUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.CommandInfo;
import com.atomist.rug.cli.command.CommandInfoRegistry;
import com.atomist.rug.cli.command.gesture.Gesture;
import com.atomist.rug.cli.command.gesture.GestureRegistry;
import com.atomist.rug.cli.output.Style;

public class CommandHelpFormatter {

    public static String HELP_FOOTER = "\n\nPlease report issues at https://github.com/atomist/rug-cli";

    public static String SHELL_HELP_FOOTER = String.format(
            "\n\nThis shell supports event expansion with '!' and executing other commands by prefixing the command with '%s', eg. '%scd .atomist && ls -la'.",
            Constants.SHELL_ESCAPE, Constants.SHELL_ESCAPE);

    private static int WRAP = Constants.WRAP_LENGTH;

    public String createString(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, ' ');
        return "\n" + new String(chars);
    }

    public String printCommandHelp(CommandInfo description) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Usage: %s%s\n", Style.bold(Constants.command()),
                Style.bold(description.usage())));
        if (!description.aliases().isEmpty()) {
            sb.append(String.format("%s: %s\n",
                    com.atomist.rug.cli.utils.StringUtils.puralize("Alias", "Aliases",
                            description.aliases()),
                    Style.bold(
                            StringUtils.collectionToDelimitedString(description.aliases(), ", "))));
        }
        sb.append(String.format("%s.\n", description.description()));

        printOptions(description.globalOptions(), sb, Style.bold("Options"));
        printOptions(description.options(), sb, Style.bold("Command Options"));

        sb.append("\n");
        sb.append(WordUtils.wrap(description.detail(), WRAP));
        sb.append(WordUtils.wrap(HELP_FOOTER, WRAP));

        return sb.toString();

    }

    public String printGestureHelp(Gesture gesture) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Usage: %s%s\n", Style.bold(Constants.command()),
                Style.bold(gesture.usage())));
        sb.append(String.format("%s.\n", gesture.description()));

        sb.append("\n");
        sb.append(WordUtils.wrap(gesture.detail(), WRAP));
        sb.append(WordUtils.wrap(HELP_FOOTER, WRAP));

        return sb.toString();
    }

    public String printHelp(CommandInfoRegistry commandRegistry, GestureRegistry gestureRegistry,
            Options options) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Usage: %s\n",
                Style.bold(Constants.command() + "[OPTION]... [COMMAND]...")));
        sb.append("Work with Rugs like editors or generators.\n");

        printOptions(options, sb, Style.bold("Options"));

        printCommands(sb, commandRegistry);
        printGestures(sb, gestureRegistry);

        sb.append("\n");
        sb.append(String.format("Run '%sCOMMAND --help' for more detailed information on COMMAND.",
                Constants.command()));

        if (Constants.isShell()) {
            sb.append(WordUtils.wrap(SHELL_HELP_FOOTER, WRAP));
        }
        sb.append(WordUtils.wrap(HELP_FOOTER, WRAP));

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
        sb.append("\n");
        sb.append(Style.bold("Available commands:"));
        int length = registry.commands().stream()
                .max(Comparator.comparingInt(o -> o.name().length())).get().name().length()
                + 1;
        String formatString = "  %-" + length + "s %s\n";

        Map<String, List<CommandInfo>> commands = registry.commands().stream()
                .collect(Collectors.groupingBy(CommandInfo::group));
        commands.entrySet().forEach(e -> {
            sb.append("\n");
            e.getValue().forEach(c -> sb.append(String.format(formatString, c.name(), WordUtils
                    .wrap(c.description(), WRAP - length, createString(length + 3), false))));
        });
    }

    private void printGestures(StringBuilder sb, GestureRegistry registry) {
        sb.append("\n");
        sb.append(Style.bold("Available gestures:"));
        sb.append("\n");
        int length = registry.gestures().stream()
                .max(Comparator.comparingInt(o -> o.name().length())).get().name().length() + 1;
        String formatString = "  %-" + length + "s %s\n";

        registry.gestures().forEach(g -> {
            sb.append(String.format(formatString, g.name(), WordUtils.wrap(g.description(),
                    WRAP - length, createString(length + 3), false)));
        });
    }

    private void printOptions(Options options, StringBuilder sb, String label) {
        if (options.getOptions().size() == 0) {
            return;
        }

        sb.append("\n");

        int length = getOptionLenght(options.getOptions().stream()
                .max(Comparator.comparingInt(this::getOptionLenght)).get()) + 6;
        String formatString = "  %-" + length + "s %s\n";

        sb.append(label).append(":\n");
        options.getOptions().stream().collect(Collectors.groupingBy(Option::getDescription))
                .entrySet().stream().sorted(new Comparator<Map.Entry<String, List<Option>>>() {

                    @Override
                    public int compare(Entry<String, List<Option>> e1,
                            Entry<String, List<Option>> e2) {
                        Option o1 = e1.getValue().get(0);
                        Option o2 = e2.getValue().get(0);

                        if (o1.getOpt() == null && o2.getOpt() != null) {
                            return 1;
                        }
                        else if (o1.getOpt() != null && o2.getOpt() == null) {
                            return -1;
                        }
                        else if (o1.getOpt() == null && o2.getOpt() == null) {
                            return o1.getLongOpt().compareTo(o2.getLongOpt());
                        }
                        else {
                            return o1.getOpt().compareTo(o2.getOpt());
                        }
                    }
                }).forEach(e -> {
                    Option opt = e.getValue().stream().findFirst().get();
                    String ops = e.getValue().stream()
                            .map(o -> (o.getOpt() != null ? "-" + o.getOpt() : null))
                            .filter(o -> o != null).collect(Collectors.joining(","));

                    if (ops.length() > 0) {
                        ops += ",";
                    }
                    ops += "--" + opt.getLongOpt();
                    if (opt.hasArgName()) {
                        ops += " " + opt.getArgName();
                    }
                    sb.append(String.format(formatString, ops, WordUtils.wrap(opt.getDescription(),
                            WRAP - length, createString(length + 3), false)));

                });
    }
}
