package com.atomist.rug.cli.command;

import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.atomist.rug.cli.command.utils.ParseExceptionProcessor;
import com.atomist.rug.cli.output.ConsoleUtils;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.resolver.ArtifactDescriptor;

public abstract class AbstractCommand implements com.atomist.rug.cli.command.Command {

    protected CommandInfoRegistry registry = new ServiceLoadingCommandInfoRegistry();

    public final void run(String... args) {

        ConsoleUtils.configureStreams();
        CommandLine commandLine = parseCommandLine(args);

        validate(null, commandLine);
        run(null, commandLine);
    }

    @Override
    public final void run(ArtifactDescriptor artifact, String... args) {

        ConsoleUtils.configureStreams();
        CommandLine commandLine = parseCommandLine(args);

        validate(artifact, commandLine);
        run(artifact, commandLine);
    }

    private CommandLine parseCommandLine(String... args) {
        args = CommandUtils.firstCommand(args);
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        try {

            Optional<CommandInfo> commandInfo = registry.commands().stream()
                    .filter(c -> c.className().equals(getClass().getName())).findFirst();
            if (commandInfo.isPresent()) {
                Options options = new Options();
                commandInfo.get().options().getOptions().forEach(options::addOption);
                commandInfo.get().globalOptions().getOptions().forEach(options::addOption);
                commandLine = parser.parse(options, args);
                CommandLineOptions.set(commandLine);
            }
            return commandLine;
        }
        catch (ParseException e) {
            throw new CommandException(ParseExceptionProcessor.process(e), (String) null);
        }
    }

    protected void validate(ArtifactDescriptor artifact, CommandLine commandLine) {
    }

    protected abstract void run(ArtifactDescriptor artifact, CommandLine commandLine);
}
