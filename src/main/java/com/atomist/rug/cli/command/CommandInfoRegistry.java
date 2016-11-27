package com.atomist.rug.cli.command;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public interface CommandInfoRegistry {

    Options allOptions();

    List<CommandInfo> commands();

    CommandInfo findCommand(CommandLine commandLine);
}