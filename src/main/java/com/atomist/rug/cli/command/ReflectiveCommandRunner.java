package com.atomist.rug.cli.command;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.classloading.ClassLoaderFactory;
import com.atomist.rug.cli.classloading.ClasspathEntryProvider;
import com.atomist.rug.cli.command.utils.DependencyResolverExceptionProcessor;
import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.resolver.DependencyResolverFactory;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.version.VersionUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptorFactory;
import com.atomist.rug.resolver.DependencyResolver;
import com.atomist.rug.resolver.DependencyResolverException;

public class ReflectiveCommandRunner {

    private final CommandInfoRegistry registry;

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
                ClassLoaderFactory.setupClassLoader(artifact, dependencies, (ClasspathEntryProvider) info);
            }
            else {
                ClassLoaderFactory.setupClassLoader(rootArtifact, dependencies);
            }
        }

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
