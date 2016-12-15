package com.atomist.rug.cli.command;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;

import com.atomist.rug.cli.RunnerException;
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
            VersionUtils.validateRugCompatibility(dependencies);

            artifact = dependencies.stream()
                    .filter(a -> a.group().equals(rootArtifact.group())
                            && a.artifact().equals(rootArtifact.artifact()))
                    .findFirst().orElse(rootArtifact);

            // Setup the new classloader for the command to execute in
            createClassLoader(artifact, dependencies, info);
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

    private void createClassLoader(ArtifactDescriptor artifact,
            List<ArtifactDescriptor> dependencies, CommandInfo commandInfo) {
        List<URL> urls = getDependencies(dependencies);

        // Add the url to the enclosing JAR
        URL codeLocation = getClass().getProtectionDomain().getCodeSource().getLocation();
        urls.add(codeLocation);

        addExtensionsToClasspath(urls);
        addCommandExtensionsToClasspath(artifact, commandInfo, urls);

        ClassLoader cls = null;
        if (codeLocation.toString().endsWith("jar")) {
            // If running from an IDE we need a different classloader hierarchy
            cls = new NashornDelegatingUrlClassLoader(urls.toArray(new URL[urls.size()]),
                    Thread.currentThread().getContextClassLoader());
        }
        else {
            cls = new URLClassLoader(urls.toArray(new URL[urls.size()]));
        }
        Thread.currentThread().setContextClassLoader(cls);
    }

    private void addCommandExtensionsToClasspath(ArtifactDescriptor artifact,
            CommandInfo commandInfo, List<URL> urls) {
        if (commandInfo instanceof ClasspathEntryProvider) {
            urls.addAll(((ClasspathEntryProvider) commandInfo).classpathEntries(artifact));
        }
    }

    private void addExtensionsToClasspath(List<URL> urls) {
        File extDir = new File(FileUtils.getUserDirectory(), ".atomist/ext");
        if (extDir.exists() && extDir.isDirectory()) {
            FileUtils.listFiles(extDir, new String[] { "jar" }, true).forEach(f -> {
                try {
                    urls.add(f.toURI().toURL());
                }
                catch (MalformedURLException e) {
                }
            });
        }
    }

    private List<URL> getDependencies(List<ArtifactDescriptor> dependencies) {
        return dependencies.stream().map(ad -> new File(ad.uri())).map(f -> {
            try {
                return f.toURI().toURL();
            }
            catch (MalformedURLException e) {
                throw new RunnerException("Error occured creating URL", e);
            }
        }).collect(Collectors.toList());
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
            throw new CommandException(DependencyResolverExceptionProcessor.process(e));
        }
    }

    private static class NashornDelegatingUrlClassLoader extends URLClassLoader {

        private ClassLoader parent;

        public NashornDelegatingUrlClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, null);
            this.parent = parent;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // Nashorn and some of the scripting classes need to come from the system classloader;
            // everything else we need to isolate and not delegate to the parent class loader
            if (name.startsWith("org.slf4j") || name.startsWith("jdk.nashorn")
                    || name.startsWith("javax.scripting")) {
                return parent.loadClass(name);
            }
            else {
                return super.loadClass(name);
            }
        }
    }
}
