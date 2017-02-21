package com.atomist.rug.cli.command;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.utils.StringUtils;

public class ServiceLoadingCommandInfoRegistry implements CommandInfoRegistry {

    private List<CommandInfo> commands = new ArrayList<>();

    public ServiceLoadingCommandInfoRegistry() {
        init();
    }

    public Options allOptions() {
        Options options = new Options();
        commands.forEach(e -> {
            e.options().getOptions().forEach(options::addOption);
            e.globalOptions().getOptions().forEach(options::addOption);
        });
        return options;
    }

    public List<CommandInfo> commands() {
        return commands;
    }

    public CommandInfo findCommand(CommandLine commandLine) {
        if (commandLine.getArgList().size() >= 1) {
            String commandName = commandLine.getArgList().get(0);
            Optional<CommandInfo> helper = commands.stream()
                    .filter(c -> c.name().equals(commandName) || c.aliases().contains(commandName))
                    .findFirst();
            if (helper.isPresent()) {
                return helper.get();
            }
            throw new CommandException(String.format("%s is not a recognized command.%s",
                    commandName, getAddtionalHelpMessage(commandName, commandLine)), (String) null);
        }
        throw new CommandException("Command is required.", (String) null);
    }

    private void init() {
        ServiceLoader<CommandInfo> loader = ServiceLoader.load(CommandInfo.class);
        loader.forEach(c -> commands.add(c));
        commands = commands.stream().sorted(Comparator.comparingInt(CommandInfo::order))
                .collect(toList());
    }

    private String getAddtionalHelpMessage(String commandName, CommandLine commandLine) {
        Optional<String> closestMatch = StringUtils.computeClosestMatch(commandName,
                commands.stream().map(CommandInfo::name).collect(toList()));
        return closestMatch
                .map(s -> new StringBuilder().append("\n\nDid you mean?\n").append("  ")
                        .append(Constants.command()).append(s).append(" ").append("").toString())
                .orElse("");
    }
}
