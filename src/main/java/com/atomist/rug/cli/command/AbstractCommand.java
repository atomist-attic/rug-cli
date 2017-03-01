package com.atomist.rug.cli.command;

import com.atomist.rug.cli.command.utils.ParseExceptionProcessor;
import com.atomist.rug.cli.output.ConsoleUtils;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;
import com.atomist.rug.resolver.LocalArtifactDescriptor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.net.URI;
import java.util.Optional;

import static com.atomist.rug.resolver.ArtifactDescriptor.*;

public abstract class AbstractCommand implements com.atomist.rug.cli.command.Command {

    protected CommandInfoRegistry registry = new ServiceLoadingCommandInfoRegistry();

    public final void run(String... args) {

        ConsoleUtils.configureStreams();
        CommandLine commandLine = parseCommandLine(args);
        
        validate(null, commandLine);
        run(null, null, commandLine);
    }

    @Override
    public final void run(String group, String artifactId, String version, String extension,
            boolean local, URI[] uri, String... args) {

        ConsoleUtils.configureStreams();
        CommandLine commandLine = parseCommandLine(args);

        ArtifactDescriptor artifact = createArtifactDescriptor(group, artifactId, version,
                extension, local);
        
        validate(artifact, commandLine);
        run(uri, artifact, commandLine);
    }

    private ArtifactDescriptor createArtifactDescriptor(String group, String artifactId,
            String version, String extension, boolean local) {
        ArtifactDescriptor artifact = null;
        if (local) {
            artifact = new LocalArtifactDescriptor(group, artifactId, version,
                    Extension.valueOf(extension), Scope.COMPILE,
                    CommandUtils.getRequiredWorkingDirectory().toURI());
        }
        else {
            File archive = new File(SettingsReader.read().getLocalRepository().path(),
                    group.replace(".", File.separator) + File.separator + artifactId
                            + File.separator + version + File.separator + artifactId + "-" + version
                            + "." + extension.toLowerCase());
            artifact = new DefaultArtifactDescriptor(group, artifactId, version,
                    Extension.valueOf(extension), Scope.COMPILE, archive.toURI());
        }
        return artifact;
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

    protected abstract void run(URI[] uri, ArtifactDescriptor artifact, CommandLine commandLine);
}
