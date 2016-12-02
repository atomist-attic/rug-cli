package com.atomist.rug.cli.resolver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.repository.ConservativeProxySelector;
import org.eclipse.aether.util.repository.JreProxySelector;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReportingTransferListener;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.MavenSettings;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.CachingDependencyResolver;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;
import com.atomist.rug.resolver.DependencyResolver;
import com.atomist.rug.resolver.LocalArtifactDescriptor;
import com.atomist.rug.resolver.maven.LogDependencyVisitor;
import com.atomist.rug.resolver.maven.MavenBasedDependencyResolver;
import com.atomist.rug.resolver.maven.MavenProperties;

public class DependencyResolverFactory {

    public DependencyResolver createDependencyResolver(ArtifactDescriptor artifact,
            ProgressReporter indicator) {
        ExecutorService executorService = Executors.newFixedThreadPool(5, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });
        MavenProperties properties = MavenSettings
                .mavenProperties(CommandLineOptions.hasOption("o"));
        MavenBasedDependencyResolver resolver = new MavenBasedDependencyResolver(
                MavenSettings.repositorySystem(), properties, executorService) {

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
                    return super.createDependencyRoot(artifact);
                }
            }
        };

        resolver.setTransferListener(new ProgressReportingTransferListener(indicator));
        resolver.setProxySelector(new ConservativeProxySelector(new JreProxySelector()));
        addExclusions(resolver);

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
                        indicator.report(Style.cyan(Constants.DIVIDER) + " "
                                + Style.bold("Dependency report for %s:%s:%s", artifact.group(),
                                        artifact.artifact(), artifact.version()));
                        firstMessage = false;
                    }
                }
            }, Constants.TREE_NODE, Constants.LAST_TREE_NODE, Constants.TREE_CONNECTOR));
        }
        return wrapDependencyResolver(resolver, properties.getRepoLocation());
    }

    private void addExclusions(MavenBasedDependencyResolver resolver) {
        // This is exclusion is needed to prevent multiple versions of slf4j bindings on the
        // classpath
        // resolver.setExclusions(Collections.singletonList("ch.qos.logback:logback-classic"));
    }

    private DependencyResolver wrapDependencyResolver(DependencyResolver resolver,
            String repoHome) {
        return new CachingDependencyResolver(resolver, repoHome);
    }

}
