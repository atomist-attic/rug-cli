package com.atomist.rug.cli.resolver;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.repository.ConservativeProxySelector;
import org.eclipse.aether.util.repository.JreProxySelector;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReportingTransferListener;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.ArtifactDescriptor.Scope;
import com.atomist.rug.resolver.CachingDependencyResolver;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;
import com.atomist.rug.resolver.DependencyResolver;
import com.atomist.rug.resolver.LocalArtifactDescriptor;
import com.atomist.rug.resolver.maven.LogDependencyVisitor;
import com.atomist.rug.resolver.maven.MavenBasedDependencyResolver;
import com.atomist.rug.resolver.maven.MavenProperties;

public abstract class DependencyResolverFactory {

    public static DependencyResolver createDependencyResolver(ArtifactDescriptor artifact,
            ProgressReporter indicator, String... exclusions) {
        ExecutorService executorService = Executors.newFixedThreadPool(5, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        MavenProperties properties = MavenPropertiesFactory.create(
                CommandLineOptions.hasOption("offline"), !CommandLineOptions.hasOption("u"));
        MavenBasedDependencyResolver resolver = new MavenBasedDependencyResolver(
                MavenPropertiesFactory.repositorySystem(), properties, executorService) {

            @Override
            public String toString() {
                return super.toString() + "[MavenBasedDependencyResolver with dependency root "
                        + artifact + "]";
            }

            @Override
            protected Dependency createDependencyRoot(ArtifactDescriptor artifact) {
                if (artifact instanceof LocalArtifactDescriptor) {
                    // for the local resolve we use an empty maven artifact and add root
                    // dependencies to it
                    DefaultArtifactDescriptor newArtifact = new DefaultArtifactDescriptor(
                            "com.atomist", "rug-cli-root", "1.0.0", Extension.JAR);
                    artifact.dependencies().forEach(newArtifact::addDependency);
                    return super.createDependencyRoot(newArtifact);
                }
                else {
                    if (artifact.extension() == Extension.ZIP) {
                        // add in the metadata.json for the root archive as dependency
                        DefaultArtifactDescriptor metadataArtifact = new DefaultArtifactDescriptor(
                                artifact.group(), artifact.artifact(), artifact.version(),
                                Extension.JSON, Scope.COMPILE, "metadata", null);
                        artifact.dependencies().add(metadataArtifact);
                    }
                    return super.createDependencyRoot(artifact);
                }
            }
        };

        resolver.setTransferListener(new ProgressReportingTransferListener(indicator));
        resolver.setProxySelector(new ConservativeProxySelector(new JreProxySelector()));
        addExclusions(resolver, exclusions);

        if (CommandLineOptions.hasOption("r")) {
            resolver.addDependencyVisitor(new LogDependencyVisitor(new LogDependencyVisitor.Log() {

                private boolean firstMessage = true;

                @Override
                public void info(String message) {
                    firstMessage();
                    indicator.report("  " + message);
                }

                private void firstMessage() {
                    if (firstMessage) {
                        indicator.report(String.format("Dependency report for %s:%s (%s)",
                                artifact.group(), artifact.artifact(), artifact.version()));
                        firstMessage = false;
                    }
                }
            }, Constants.TREE_NODE, Constants.LAST_TREE_NODE, Constants.TREE_CONNECTOR,
                    Constants.TREE_NODE_WITH_CHILDREN, Constants.LAST_TREE_NODE_WITH_CHILDREN));
        }
        return wrapDependencyResolver(resolver, properties.getRepoLocation());
    }

    private static void addExclusions(MavenBasedDependencyResolver resolver, String... exclusions) {
        resolver.setExclusions(Arrays.asList(exclusions));
    }

    private static DependencyResolver wrapDependencyResolver(DependencyResolver resolver,
            String repoHome) {
        if (CommandLineOptions.hasOption("requires")) {
            return resolver;
        }
        else {
            return new CachingDependencyResolver(resolver, repoHome) {

                @Override
                public String toString() {
                    return super.toString() + "[Caching dependency resolver around " + resolver
                            + "]";
                }

                @Override
                protected boolean isOutdated(ArtifactDescriptor artifact, File file) {
                    if (CommandLineOptions.hasOption("u")) {
                        return true;
                    }
                    return super.isOutdated(artifact, file);
                }

            };
        }
    }

}
