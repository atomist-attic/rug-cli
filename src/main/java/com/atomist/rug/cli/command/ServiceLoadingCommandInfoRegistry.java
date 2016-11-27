package com.atomist.rug.cli.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

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
                    String.format("%s is not a recognized command.", commandName), null);
        }
    }

    private void init() {
        ServiceLoader<CommandInfo> loader = ServiceLoader.load(CommandInfo.class);
        loader.forEach(c -> commands.add(c));
        commands = commands.stream().sorted((c1, c2) -> Integer.compare(c1.order(), c2.order()))
                .collect(Collectors.toList());
    }
}
