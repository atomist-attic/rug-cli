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

    public CommandInfo findCommand(String name) {
        Optional<CommandInfo> helper = commands.stream()
                .filter(c -> c.name().equals(name) || c.aliases().contains(name)).findFirst();
        if (helper.isPresent()) {
            return helper.get();
        }
        throw new CommandException(String.format("%s is not a recognized command.%s", name,
                getAddtionalHelpMessage(name)), (String) null);
    }

    private void init() {
        ServiceLoader<CommandInfo> loader = ServiceLoader.load(CommandInfo.class);
        loader.forEach(c -> commands.add(c));
        commands = commands.stream().sorted(Comparator.comparingInt(CommandInfo::order))
                .collect(toList());
    }

    private String getAddtionalHelpMessage(String commandName) {
        Optional<String> closestMatch = StringUtils.computeClosestMatch(commandName,
                commands.stream().map(CommandInfo::name).collect(toList()));
        return closestMatch
                .map(s -> new StringBuilder().append("\n\nDid you mean?\n").append("  ")
                        .append(Constants.command()).append(s).append(" ").append("").toString())
                .orElse("");
    }

    @Override
    public CommandInfo findCommand(Class<? extends Command> cls) {
        String name = cls.getName();
        Optional<CommandInfo> info = commands.stream().filter(c -> c.className().equals(name))
                .findAny();
        return info.orElse(null);
    }
}
