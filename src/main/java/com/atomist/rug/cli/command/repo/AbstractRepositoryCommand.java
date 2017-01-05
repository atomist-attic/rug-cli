package com.atomist.rug.cli.command.repo;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;

import com.atomist.project.ProvenanceInfo;
import com.atomist.project.SimpleProvenanceInfo;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.deployer.AbstractMavenBasedDeployer;
import com.atomist.rug.deployer.DefaultDeployerEventListener;
import com.atomist.rug.deployer.Deployer;
import com.atomist.rug.git.RepositoryDetails;
import com.atomist.rug.git.RepositoryDetailsProvider;
import com.atomist.rug.loader.OperationsAndHandlers;
import com.atomist.rug.manifest.Manifest;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptorFactory;
import com.atomist.source.ArtifactSource;
import com.atomist.source.FileArtifact;

public abstract class AbstractRepositoryCommand extends AbstractAnnotationBasedCommand {

    private Log log = new Log(AbstractRepositoryCommand.class);

    @Command
    public void run(OperationsAndHandlers operationsAndHandlers, ArtifactDescriptor artifact,
            ArtifactSource source, @Option("archive-version") String version,
            CommandLine commandLine) throws IOException {

        String fullVersion = artifact.version();
        if (version != null) {
            fullVersion = version;
            artifact = ArtifactDescriptorFactory.copyFrom(artifact, fullVersion);
        }

        String zipFileName = artifact.artifact() + "-" + fullVersion + "."
                + artifact.extension().toString().toLowerCase();
        File projectRoot = CommandUtils.getRequiredWorkingDirectory();
        File archive = new File(projectRoot,
                Constants.ATOMIST_ROOT + File.separator + "target" + File.separator + zipFileName);

        prepareTargetDirectory(archive);
        source = ArtifactSourceUtils.filter(source);

        Deployer deployer = new RepositoryCommandMavenDeployer(commandLine, projectRoot);
        deployer.deploy(operationsAndHandlers, source, artifact, projectRoot);
    }

    protected abstract void doWithRepositorySession(RepositorySystem system,
            RepositorySystemSession session, ArtifactSource source, Manifest manifest, Artifact zip,
            Artifact pom, Artifact metadata, CommandLine commandLine);

    private void prepareTargetDirectory(File zipFile) {
        FileUtils.deleteQuietly(zipFile.getParentFile());
        if (!zipFile.getParentFile().exists()) {
            zipFile.getParentFile().mkdirs();
        }
    }

    private class ReportingDeployerEventListener extends DefaultDeployerEventListener {

        @Override
        public void metadataFileGenerated(FileArtifact file) {
            log.info("  Generated %s", file.path());

        }
    }

    private class RepositoryCommandMavenDeployer extends AbstractMavenBasedDeployer {

        private CommandLine commandLine;
        private File projectRoot;

        public RepositoryCommandMavenDeployer(CommandLine commandLine, File projectRoot) {
            super(new SettingsReader().read().getLocalRepository().path());
            this.commandLine = commandLine;
            this.projectRoot = projectRoot;
            registerEventListener(new ReportingDeployerEventListener());
        }

        @Override
        protected void doWithRepositorySession(RepositorySystem system,
                RepositorySystemSession session, ArtifactSource source, Manifest manifest,
                Artifact zip, Artifact pom, Artifact metadata) {
            AbstractRepositoryCommand.this.doWithRepositorySession(system, session, source,
                    manifest, zip, pom, metadata, commandLine);
        }

        @Override
        protected ProvenanceInfo getProvenanceInfo() {
            try {
                Optional<RepositoryDetails> repositoryDetails = new RepositoryDetailsProvider()
                        .readDetails(projectRoot);
                if (repositoryDetails.isPresent()) {
                    return new SimpleProvenanceInfo(repositoryDetails.get().repo(),
                            repositoryDetails.get().branch(), repositoryDetails.get().sha());
                }
            }
            catch (IOException e) {

            }
            return null;
        }

        @Override
        protected ArtifactSource generateMetadata(OperationsAndHandlers operationsAndHandlers,
                ArtifactDescriptor artifact, ArtifactSource source, Manifest manifest) {
            return new ProgressReportingOperationRunner<ArtifactSource>(
                    "Generating archive metadata").run(indicator -> {
                        return super.generateMetadata(operationsAndHandlers, artifact, source,
                                manifest);
                    });
        }
    }
}
