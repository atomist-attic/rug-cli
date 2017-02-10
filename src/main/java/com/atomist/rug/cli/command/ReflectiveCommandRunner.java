package com.atomist.rug.cli.command;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.FileNameCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.TerminalBuilder;
import org.springframework.util.StringUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.classloading.ClassLoaderFactory;
import com.atomist.rug.cli.classloading.ClasspathEntryProvider;
import com.atomist.rug.cli.command.utils.DependencyResolverExceptionProcessor;
import com.atomist.rug.cli.command.utils.ParseExceptionProcessor;
import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.resolver.DependencyResolverFactory;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.version.VersionUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptorFactory;
import com.atomist.rug.resolver.DependencyResolver;
import com.atomist.rug.resolver.DependencyResolverException;

public class ReflectiveCommandRunner {

    private final CommandInfoRegistry registry;
    private final Log log = new Log(ReflectiveCommandRunner.class);

    public ReflectiveCommandRunner(CommandInfoRegistry registry) {
        this.registry = registry;
    }

    public void runCommand(String[] args, CommandLine commandLine) {

        // Validate the JDK version
        VersionUtils.validateJdkVersion();

        ArtifactDescriptor artifact = null;
        List<ArtifactDescriptor> dependencies = Collections.emptyList();

        CommandInfo info = registry.findCommand(commandLine);

        if (info instanceof ArtifactDescriptorProvider) {
            ArtifactDescriptor rootArtifact = ((ArtifactDescriptorProvider) info)
                    .artifactDescriptor(commandLine);

            dependencies = new ProgressReportingOperationRunner<List<ArtifactDescriptor>>(
                    String.format("Resolving dependencies for %s",
                            ArtifactDescriptorUtils.coordinates(rootArtifact)))
                                    .run(indicator -> resolveDependencies(rootArtifact, indicator));

            // Validate that this CLI version is compatible with declared version of Rug
            VersionUtils.validateRugCompatibility(rootArtifact, dependencies);

            artifact = dependencies.stream()
                    .filter(a -> a.group().equals(rootArtifact.group())
                            && a.artifact().equals(rootArtifact.artifact()))
                    .findFirst().orElse(rootArtifact);

            // Setup the new classloader for the command to execute in
            if (info instanceof ClasspathEntryProvider) {
                ClassLoaderFactory.setupClassLoader(artifact, dependencies,
                        (ClasspathEntryProvider) info);
            }
            else {
                ClassLoaderFactory.setupClassLoader(rootArtifact, dependencies);
            }
        }

        invokeCommand(args, artifact, dependencies, info);
        if ("shell".equals(info.name())) {
            try {
                
                // TODO this has to go to somewhere else and become resuable
                History history = new DefaultHistory();
                LineReader reader = LineReaderBuilder.builder().terminal(TerminalBuilder.builder().build())
                        .history(history)
                        .variable(LineReader.HISTORY_FILE,
                                new File(System.getProperty("user.home") + File.separator
                                        + ".atomist" + File.separator + ".cli-history"))
                        .completer(new AggregateCompleter(new StringsCompleter("edit", "generate", "describe", "list",
                                "search", "install", "test", "publish", "archive", "editor", "generator"), new FileNameCompleter()))
                        .highlighter(new DefaultHighlighter())
                        .build();
                history.attach(reader);
                String prompt = Style.yellow("rug") + " " + Style.cyan(Constants.DIVIDER) + " ";

                while (true) {
                    String line = null;
                    try {
                        log.newline();
                        line = reader.readLine(prompt);
                        
                        if ("exit".equals(line)) {
                            throw new EndOfFileException();
                        }
                        
                        args = StringUtils.tokenizeToStringArray(line, " ");

                        commandLine = parseCommandline(args);
                        info = registry.findCommand(commandLine);

                        invokeCommand(args, artifact, dependencies, info);
                    }
                    catch (UserInterruptException e) {
                    }
                    catch (EndOfFileException e) {
                        log.info("Goodbye!");
                        return;
                    }
                    finally {
                        history.save();
                    }
                }
            }
            catch (IOException e1) {
                // TODO handle
            }
        }
    }

    // TODO fixme this shouldn't be here.
    private CommandLine parseCommandline(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(registry.allOptions(), args);
            CommandLineOptions.set(commandLine);
            return commandLine;
        }
        catch (ParseException e) {
            throw new CommandException(ParseExceptionProcessor.process(e), (String) null);
        }
    }

    private void invokeCommand(String[] args, ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies, CommandInfo info) {
        try {
            // Invoke the run method on the command class
            new ReflectiveCommandRunMethodRunner().invokeCommand(artifact, info, args,
                    getZipDependencies(dependencies));
        }
        catch (NoClassDefFoundError | ClassNotFoundException e) {
            throw new RunnerException(
                    "Could not execute command. Likely because specified archive is missing required dependencies",
                    e);
        }
        catch (RunnerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RunnerException(e);
        }
    }

    private List<URI> getZipDependencies(List<ArtifactDescriptor> dependencies) {
        return dependencies.stream().map(ad -> new File(ad.uri()))
                .filter(f -> f.getName().endsWith(".zip")).map(File::toURI)
                .collect(Collectors.toList());
    }

    private List<ArtifactDescriptor> resolveDependencies(ArtifactDescriptor artifact,
            ProgressReporter indicator) {
        DependencyResolver resolver = new DependencyResolverFactory()
                .createDependencyResolver(artifact, indicator);
        String version = artifact.version();
        try {
            version = resolver.resolveVersion(artifact);
            return resolver.resolveTransitiveDependencies(
                    ArtifactDescriptorFactory.copyFrom(artifact, version));
        }
        catch (DependencyResolverException e) {
            throw new CommandException(DependencyResolverExceptionProcessor
                    .process(ArtifactDescriptorFactory.copyFrom(artifact, version), e));
        }
    }

}
