package com.atomist.rug.cli.command;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.springframework.util.StringUtils;

import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.classloading.ClassLoaderFactory;
import com.atomist.rug.cli.classloading.ClasspathEntryProvider;
import com.atomist.rug.cli.command.shell.ChangeDirCompleter;
import com.atomist.rug.cli.command.shell.CommandInfoCompleter;
import com.atomist.rug.cli.command.shell.OperationCompleter;
import com.atomist.rug.cli.command.shell.ShellUtils;
import com.atomist.rug.cli.command.utils.CommandHelpFormatter;
import com.atomist.rug.cli.command.utils.DependencyResolverExceptionProcessor;
import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.resolver.DependencyResolverFactory;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.Timing;
import com.atomist.rug.cli.version.VersionUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptorFactory;
import com.atomist.rug.resolver.DependencyResolver;
import com.atomist.rug.resolver.DependencyResolverException;

public class ReflectiveCommandRunner {

    private final Log log = new Log(ReflectiveCommandRunner.class);
    private final CommandInfoRegistry registry;

    public ReflectiveCommandRunner(CommandInfoRegistry registry) {
        this.registry = registry;
    }

    public int runCommand(String[] args, CommandLine commandLine) {

        Timing timing = new Timing();

        if (commandLine.hasOption("?") || commandLine.hasOption("h")) {
            printCommandHelp(commandLine);
            return 0;
        }

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

        int rc = invokeCommand(args, artifact, dependencies, timing);

        if (rc == 0 && "shell".equals(info.name())) {
            LineReader reader = ShellUtils.lineReader(ShellUtils.SHELL_HISTORY,
                    new ChangeDirCompleter(), new OperationCompleter(),
                    new CommandInfoCompleter(registry));
            promptLoop(artifact, dependencies, reader);
        }

        return rc;
    }

    private void promptLoop(ArtifactDescriptor artifact, List<ArtifactDescriptor> dependencies,
            LineReader reader) {
        String[] args;
        while (true) {
            String line = null;
            try {
                line = reader.readLine(ShellUtils.DEFAULT_PROMPT);

                if ("exit".equals(line.trim()) || "q".equals(line.trim())) {
                    throw new EndOfFileException();
                }
                
                args = CommandUtils.splitCommandline(line);
                invokeCommand(args, artifact, dependencies, null);
            }
            catch (UserInterruptException e) {
            }
            catch (EndOfFileException e) {
                log.info("Goodbye!");
                return;
            }
        }
    }

    private Throwable extractRootCause(Throwable t) {
        if (t instanceof InvocationTargetException) {
            return extractRootCause(((InvocationTargetException) t).getTargetException());
        }
        else if (t instanceof CommandException) {
            return t;
        }
        else if (t instanceof RuntimeException) {
            if (t.getCause() != null) {
                return extractRootCause(t.getCause());
            }
        }
        return t;
    }

    private List<URI> getZipDependencies(List<ArtifactDescriptor> dependencies) {
        return dependencies.stream().map(ad -> new File(ad.uri()))
                .filter(f -> f.getName().endsWith(".zip")).map(File::toURI)
                .collect(Collectors.toList());
    }

    private int invokeCommand(String[] args, ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies, Timing timing) {

        if (timing == null) {
            timing = new Timing();
        }

        CommandLine commandLine = null;
        try {
            commandLine = CommandUtils.parseCommandline(args, registry);
            CommandInfo info = registry.findCommand(commandLine);

            if (commandLine.hasOption("?") || commandLine.hasOption("h")) {
                printCommandHelp(commandLine);
                return 0;
            }
            else {
                invokeReflectiveCommand(args, artifact, dependencies, info);
            }
        }
        catch (Throwable e) {
            // Extract root exception; cycle through nested exceptions to extract root cause
            e = extractRootCause(e);
            
            // Print stacktraces only if requested from the command line
            log.newline();
            if (commandLine != null && commandLine.hasOption('X')) {
                log.error(e);
            }
            else {
                log.error(e.getMessage());
            }
            return 1;
        }
        finally {
            if (commandLine != null && commandLine.hasOption('t')) {
                printTimer(timing);
            }
        }
        return 0;
    }

    private void invokeReflectiveCommand(String[] args, ArtifactDescriptor artifact,
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

    private void printTimer(Timing timing) {
        log.info("Command completed in " + timing.duration() + "s");
    }

    private void printCommandHelp(CommandLine commandLine) {
        new Log(getClass()).info(
                new CommandHelpFormatter().printCommandHelp(registry.findCommand(commandLine)));
    }

}
