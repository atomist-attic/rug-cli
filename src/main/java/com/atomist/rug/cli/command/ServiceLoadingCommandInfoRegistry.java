package com.atomist.rug.cli.command;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.apache.commons.cli.Options;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.utils.StringUtils;

public class ServiceLoadingCommandInfoRegistry implements CommandInfoRegistry {

    private List<CommandInfo> commands = new ArrayList<>();

    public ServiceLoadingCommandInfoRegistry() {
        init();
    }

    @Override
    public Options allOptions() {
        Options options = new Options();
        commands.forEach(e -> {
            e.options().getOptions().forEach(options::addOption);
            e.globalOptions().getOptions().forEach(options::addOption);
        });
        return options;
    }

    @Override
    public Options options(String name) {
        Options options = new Options();
        commands.stream().filter(c -> c.name().equals(name)).forEach(e -> {
            e.options().getOptions().forEach(options::addOption);
            e.globalOptions().getOptions().forEach(options::addOption);
        });
        return options;
    }

    public List<CommandInfo> commands() {
        return commands;
    }

    public CommandInfo findCommand(String[] args) {
        String name = null;
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("-")) {
                if (name == null) {
                    name = args[i];
                }
                else {
                    name = name + " " + args[i];
                }

                Optional<CommandInfo> info = findCommand(name);
                if (info.isPresent()) {
                    return info.get();
                }
            }
        }

        throw new CommandException(
                String.format("No recognized command provided.%s", getAddtionalHelpMessage(args)),
                (String) null);
    }

    private Optional<CommandInfo> findCommand(String name) {
        return commands.stream().filter(c -> c.name().equals(name) || c.aliases().contains(name))
                .findFirst();
    }

    private void init() {
        ServiceLoader<CommandInfo> loader = ServiceLoader.load(CommandInfo.class);
        loader.forEach(c -> commands.add(c));
        commands = commands.stream().sorted(Comparator.comparingInt(CommandInfo::order))
                .collect(toList());
    }

    private String getAddtionalHelpMessage(String[] args) {
        String name = null;

        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                name = args[i];
            }
            else {
                name = name + " " + args[i];
            }

            Optional<String> closestMatch = StringUtils.computeClosestMatch(name,
                    commands.stream().map(CommandInfo::name).collect(toList()));
            if (closestMatch.isPresent()) {
                return closestMatch.map(s -> new StringBuilder().append("\n\nDid you mean?\n")
                        .append("  ").append(Constants.command()).append(s).append(" ").append("")
                        .toString()).orElse("");
            }
        }
        return "";
    }

    @Override
    public CommandInfo findCommand(Class<? extends Command> cls) {
        String name = cls.getName();
        Optional<CommandInfo> info = commands.stream().filter(c -> c.className().equals(name))
                .findAny();
        return info.orElse(null);
    }
}
