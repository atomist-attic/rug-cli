package com.atomist.rug.cli.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.utils.StringUtils;

import static java.util.stream.Collectors.toList;

public class ServiceLoadingCommandInfoRegistry implements CommandInfoRegistry {

    private List<CommandInfo> commands = new ArrayList<>();

    public ServiceLoadingCommandInfoRegistry() {
        init();
    }

    public Options allOptions() {
        Options options = new Options();
        commands.forEach(e -> {
            e.options().getOptions().stream().forEach(options::addOption);
            e.globalOptions().getOptions().stream().forEach(options::addOption);
        });
        return options;
    }

    public List<CommandInfo> commands() {
        return commands;
    }

    public CommandInfo findCommand(CommandLine commandLine) {
        String commandName = commandLine.getArgList().get(0);
        Optional<CommandInfo> helper = commands.stream().filter(c -> c.name().equals(commandName))
                .findFirst();
        if (helper.isPresent()) {
            return helper.get();
        }
        else {
            throw new CommandException(
                    String.format("%s is not a recognized command.%s", commandName,
                            getAddtionalHelpMessage(commandName, commandLine)), (String) null);
        }
    }

    private void init() {
        ServiceLoader<CommandInfo> loader = ServiceLoader.load(CommandInfo.class);
        loader.forEach(c -> commands.add(c));
        commands = commands.stream().sorted((c1, c2) -> Integer.compare(c1.order(), c2.order()))
                .collect(toList());
    }

    private String getAddtionalHelpMessage(String commandName, CommandLine commandLine) {
        Optional<String> closestMatch = StringUtils.computeClosestMatch(commandName,
                commands.stream().map(CommandInfo::name).collect(toList()));
        if (closestMatch.isPresent()) {
            return new StringBuilder()
                    .append("\n\nDid you mean?\n")
                    .append("  ")
                    .append(Constants.COMMAND)
                    .append(" ")
                    .append(closestMatch.get())
                    .append(" ")
                    .append("")
                    .toString();
        }
        else {
            return "";
        }
    }
}
