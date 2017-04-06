package com.atomist.rug.cli.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.classloading.ClassLoaderFactory;
import com.atomist.rug.cli.classloading.ClasspathEntryProvider;
import com.atomist.rug.cli.command.utils.CommandHelpFormatter;
import com.atomist.rug.cli.command.utils.DependencyResolverExceptionProcessor;
import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.resolver.DependencyResolverFactory;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.Timing;
import com.atomist.rug.cli.version.VersionUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.ArtifactDescriptorFactory;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;
import com.atomist.rug.resolver.DependencyResolver;
import com.atomist.rug.resolver.DependencyResolverException;
import com.atomist.rug.resolver.LocalArtifactDescriptor;

public class ReflectiveCommandRunner {

    protected final Log log = new Log(getClass());
    private final CommandInfoRegistry registry;
    private ClassLoader classLoader;

    public ReflectiveCommandRunner(CommandInfoRegistry registry) {
        this.registry = registry;
    }

    public int runCommand(String commandName, String[] args) {

        Timing timing = new Timing();

        CommandInfo info = registry.findCommand(commandName);
        CommandLine commandLine = CommandUtils.parseCommandline(info.name(), args, registry);

        if (commandLine.hasOption("?") || commandLine.hasOption("h")) {
            printCommandHelp(commandName);
            return 0;
        }

        List<ArtifactDescriptor> dependencies = new ArrayList<>();

        ArtifactDescriptor artifact = loadArtifactAndinitializeEnvironment(commandLine,
                dependencies, info);
        try {
            int rc = invokeCommand(commandName, args, artifact, dependencies, timing, false);

            commandCompleted(rc, args, info, artifact, dependencies);

            return rc;
        }
        finally {
            // Restore old class loader
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
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

    private void invokeReflectiveCommand(String[] args, ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies, CommandInfo info) {
        try {
            // Invoke the run method on the command class
            new ReflectiveCommandRunMethodRunner().invokeCommand(artifact, info, args);
        }
        catch (NoClassDefFoundError | ClassNotFoundException e) {
            throw new RunnerException(e);
        }
        catch (RunnerException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RunnerException(e);
        }
    }

    private ArtifactDescriptor loadArtifactAndinitializeEnvironment(CommandLine commandLine,
            List<ArtifactDescriptor> dependencies, CommandInfo info) {
        ArtifactDescriptor artifact = null;
        if (info instanceof ArtifactDescriptorProvider) {
            ArtifactDescriptor rootArtifact = ((ArtifactDescriptorProvider) info)
                    .artifactDescriptor(commandLine);

            artifact = new ProgressReportingOperationRunner<ArtifactDescriptor>(
                    String.format("Resolving dependencies for %s",
                            ArtifactDescriptorUtils.coordinates(rootArtifact))).run(indicator -> {
                                DependencyResolver resolver = DependencyResolverFactory
                                        .createDependencyResolver(rootArtifact, indicator);
                                dependencies.addAll(
                                        resolveDependencies(resolver, rootArtifact, indicator));
                                return resolveRugs(resolver, rootArtifact);
                            });

            // Validate that this CLI version is compatible with declared version of Rug
            VersionUtils.validateRugCompatibility(rootArtifact, dependencies);

            ClassLoaderFactory.setupJ2V8ClassLoader(dependencies);

            // Hold on to old class loader
            classLoader = Thread.currentThread().getContextClassLoader();

            // Setup the new classloader for the command to execute in
            if (info instanceof ClasspathEntryProvider) {
                ClassLoaderFactory.setupClassLoader(artifact, dependencies,
                        (ClasspathEntryProvider) info);
            }
            else {
                ClassLoaderFactory.setupClassLoader(rootArtifact, dependencies);
            }

            List<ArtifactDescriptor> artifactDependencies = artifact.dependencies();
            if (rootArtifact instanceof LocalArtifactDescriptor) {
                artifact = new LocalArtifactDescriptor(rootArtifact.group(),
                        rootArtifact.artifact(), rootArtifact.version(), rootArtifact.extension(),
                        rootArtifact.scope(), rootArtifact.uri());
            }
            else {
                artifact = new DefaultArtifactDescriptor(rootArtifact.group(),
                        rootArtifact.artifact(), artifact.version(), rootArtifact.extension(),
                        rootArtifact.scope(), rootArtifact.classifier(), artifact.uri());
            }
            artifact.dependencies().addAll(artifactDependencies);
        }
        return artifact;
    }

    private void printCommandHelp(String commandName) {
        new Log(getClass()).info(
                new CommandHelpFormatter().printCommandHelp(registry.findCommand(commandName)));
    }

    private void printError(CommandLine commandLine, Throwable e) {
        printError((commandLine != null && commandLine.hasOption('X')), e);
    }

    protected void printError(boolean printStacktrace, Throwable e) {
        // Extract root case for potentially wrapped exception because of reflective calls
        e = extractRootCause(e);
        // Print stacktraces only if requested from the command line
        if (printStacktrace) {
            log.error(e);
        }
        else {
            log.error(e.getMessage());
        }
    }

    private void printTimer(Timing timing) {
        log.info("Command completed in " + String.format("%.2f", timing.duration()) + "s");
    }

    protected ArtifactDescriptor resolveRugs(DependencyResolver resolver,
            ArtifactDescriptor artifact) {
        String version = resolver.resolveVersion(artifact);
        return resolver.resolveRugs(ArtifactDescriptorFactory.copyFrom(artifact, version));
    }

    protected List<ArtifactDescriptor> resolveDependencies(DependencyResolver resolver,
            ArtifactDescriptor artifact, ProgressReporter indicator) {
        String version = artifact.version();
        try {
            List<ArtifactDescriptor> dependencies = new ArrayList<>();

            // Add the rug dependency manually and make sure it is not resolved from the archive
            Optional<String> versionOverride = CommandLineOptions.getOptionValue("requires");
            if (versionOverride.isPresent()) {
                dependencies.addAll(
                        resolver.resolveDependencies(new DefaultArtifactDescriptor(Constants.GROUP,
                                Constants.RUG_ARTIFACT, versionOverride.get(), Extension.JAR)));

                // Creating a new resolver that excludes the rug dependency
                resolver = DependencyResolverFactory.createDependencyResolver(artifact, indicator,
                        "com.atomist:rug");
            }

            version = resolver.resolveVersion(artifact);
            dependencies.addAll(resolver
                    .resolveDependencies(ArtifactDescriptorFactory.copyFrom(artifact, version)));
            return dependencies;
        }
        catch (DependencyResolverException e) {
            throw new CommandException(DependencyResolverExceptionProcessor
                    .process(ArtifactDescriptorFactory.copyFrom(artifact, version), e));
        }
    }

    protected void commandEnabled(ArtifactDescriptor artifact, CommandInfo info) {
        if (!info.enabled(artifact)) {
            throw new CommandException(String.format(
                    "Command %s currently not enabled because no archive is loaded.\nPlease load an archive into this shell by running:\n  shell group:artifact",
                    info.name()), (String) null);
        }
    }

    protected void artifactChanged(ArtifactDescriptor artifact, CommandInfo info,
            CommandLine commandLine) {
    }

    protected void commandCompleted(int rc, String[] args, CommandInfo info,
            ArtifactDescriptor artifact, List<ArtifactDescriptor> dependencies) {
    }

    protected int invokeCommand(String commandName, String[] args, ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies) {
        return invokeCommand(commandName, args, artifact, dependencies, new Timing(), true);
    }

    protected int invokeCommand(String commandName, String[] args, ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies, Timing timing, boolean checkArtifact) {

        CommandLine commandLine = null;
        try {
            CommandInfo info = registry.findCommand(commandName);
            commandLine = CommandUtils.parseCommandline(info.name(), args, registry);

            // verify that command is enabled
            commandEnabled(artifact, info);

            // check the artifact against the previous one
            if (checkArtifact) {
                artifactChanged(artifact, info, commandLine);
            }

            if (commandLine.hasOption("?") || commandLine.hasOption("h")) {
                printCommandHelp(commandName);
                return 0;
            }
            else {
                invokeReflectiveCommand(args, artifact, dependencies, info);
            }
        }
        catch (Throwable e) {
            // Extract root exception; cycle through nested exceptions to extract root cause
            printError(commandLine, e);
            return 1;
        }
        finally {
            if (commandLine != null && commandLine.hasOption('t')) {
                printTimer(timing);
            }
        }
        return 0;
    }

}
