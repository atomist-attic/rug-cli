package com.atomist.rug.cli.resolver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bouncycastle.openpgp.PGPException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.repository.ConservativeProxySelector;
import org.eclipse.aether.util.repository.JreProxySelector;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReporterUtils;
import com.atomist.rug.cli.output.ProgressReportingTransferListener;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.ArtifactDescriptor.Scope;
import com.atomist.rug.resolver.CachingDependencyResolver;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;
import com.atomist.rug.resolver.DependencyResolver;
import com.atomist.rug.resolver.DependencyVerificationListener;
import com.atomist.rug.resolver.DependencyVerifier;
import com.atomist.rug.resolver.GpgSignatureVerifier;
import com.atomist.rug.resolver.LocalArtifactDescriptor;
import com.atomist.rug.resolver.maven.LogDependencyVisitor;
import com.atomist.rug.resolver.maven.MavenBasedDependencyResolver;
import com.atomist.rug.resolver.maven.MavenProperties;

public abstract class DependencyResolverFactory {

    private static final Log log = new Log(DependencyResolverFactory.class);

    public static DependencyVerifier[] verifiers() {
        if (!CommandLineOptions.hasOption("disable-verification")) {
            try {
                return new DependencyVerifier[] { new GpgSignatureVerifier() };
            }
            catch (IOException | PGPException e) {
                throw new RunnerException(e);
            }
        }
        else {
            log.info(Style.yellow(
                    "Extension verification is disabled. Please use with extreme caution!"));
        }
        return new DependencyVerifier[0];
    }

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

            @Override
            protected boolean shouldVerify(DependencyNode node, DependencyNode parent) {
                if (parent == null) {
                    return false;
                }
                Artifact parentArtifact = parent.getArtifact();
                if (parentArtifact.getGroupId().equals("com.atomist")
                        && parentArtifact.getArtifactId().equals("rug-cli-root")
                        && node.getArtifact().getExtension().equals("jar")) {
                    return true;
                }
                if (node.getArtifact().getExtension().equals("jar")
                        && parent.getArtifact().getExtension().equals("zip")) {
                    return true;
                }
                else {
                    return false;
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

        resolver.addDependencyVerificationListener(new ReportingDependencyVerificationListener());
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

    private static class ReportingDependencyVerificationListener
            implements DependencyVerificationListener {

        private Log log = new Log(ReportingDependencyVerificationListener.class);
        private StringBuilder sb;

        @Override
        public void starting(String group, String artifact, String version) {
            sb = new StringBuilder();
            sb.append(String.format("  Verifying integrity of %s:%s (%s) ", group, artifact,
                    version));
        }

        @Override
        public void succeeded(String group, String artifact, String version) {
            sb.append(Style.green("succeeded"));
            if (CommandLineOptions.hasOption("n")) {
                log.info(sb.toString());
            }
            else {
                ProgressReporterUtils.detail(String.format("%s:%s (%s%sverified)", group, artifact,
                        version, Constants.DOT));
            }
        }

        @Override
        public void failed(String group, String artifact, String version, Exception e) {
            sb.append(Style.red("failed"));
            log.info(sb.toString());
        }
    }
}
