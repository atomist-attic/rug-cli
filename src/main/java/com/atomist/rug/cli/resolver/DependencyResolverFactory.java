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

import scala.collection.immutable.Stream.Cons;

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
                        if (!artifact.dependencies().stream()
                                .anyMatch(d -> d.group().equals(artifact.group())
                                        && d.artifact().equals(artifact.artifact())
                                        && d.version().equals(artifact.version())
                                        && d.scope().equals(Scope.COMPILE)
                                        && d.extension().equals(Extension.JSON)
                                        && d.classifier().equals("metadata"))) {
                            // add in the metadata.json for the root archive as dependency
                            DefaultArtifactDescriptor metadataArtifact = new DefaultArtifactDescriptor(
                                    artifact.group(), artifact.artifact(), artifact.version(),
                                    Extension.JSON, Scope.COMPILE, "metadata", null);
                            artifact.dependencies().add(metadataArtifact);
                        }
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

                private String artifactId = String.format("%s:%s (", artifact.group(),
                        artifact.artifact(), artifact.extension().toString().toLowerCase());
                private String cliArtifact = "com.atomist:rug-cli-root (1.0.0" + Constants.DOT
                        + "jar" + Constants.DOT + "compile)";
                private boolean firstMessage = true;

                @Override
                public void info(String message) {
                    if (message.contains(cliArtifact) || (message.startsWith(artifactId)
                            && message.endsWith(artifact.extension().toString().toLowerCase()
                                    + Constants.DOT + "compile)"))) {
                        heading();
                        message = message.replace(cliArtifact,
                                artifactId + artifact.version() + ")");
                    }
                    indicator.report("  " + message);
                }

                private void heading() {
                    if (firstMessage) {
                        indicator.report(String.format("Binary dependency report for %s:%s (%s)",
                                artifact.group(), artifact.artifact(), artifact.version()));
                        firstMessage = false;
                    }
                    else {
                        indicator.report(String.format("Archive dependency report for %s:%s (%s)",
                                artifact.group(), artifact.artifact(), artifact.version()));
                    }
                }
            }, Constants.TREE_NODE, Constants.LAST_TREE_NODE, Constants.TREE_CONNECTOR,
                    Constants.TREE_NODE_WITH_CHILDREN, Constants.LAST_TREE_NODE_WITH_CHILDREN,
                    Constants.DOT));
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
