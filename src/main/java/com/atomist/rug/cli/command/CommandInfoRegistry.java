package com.atomist.rug.cli.command;

import java.util.List;

import org.apache.commons.cli.Options;

public interface CommandInfoRegistry {

    Options allOptions();

    Options options(String name);

    List<CommandInfo> commands();

    CommandInfo findCommand(Class<? extends Command> obj);

    CommandInfo findCommand(String[] args);
}