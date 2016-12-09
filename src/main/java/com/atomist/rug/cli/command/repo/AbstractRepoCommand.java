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
import com.atomist.project.archive.Operations;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.deployer.AbstractMavenBasedDeployer;
import com.atomist.rug.deployer.Deployer;
import com.atomist.rug.git.RepositoryDetails;
import com.atomist.rug.git.RepositoryDetailsProvider;
import com.atomist.rug.manifest.Manifest;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptorFactory;
import com.atomist.source.ArtifactSource;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;

public abstract class AbstractRepoCommand extends AbstractAnnotationBasedCommand {

    @Command
    public void run(Operations operations, ArtifactDescriptor artifact,
            @Option("archive-version") String version, CommandLine commandLine) throws IOException {
        
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
        ArtifactSource source = createArtifactSource(projectRoot);

        Deployer deployer = new AbstractMavenBasedDeployer(
                new SettingsReader().read().getLocalRepository().path()) {

            @Override
            protected void doWithRepositorySession(RepositorySystem system,
                    RepositorySystemSession session, ArtifactSource source, Manifest manifest,
                    Artifact zip, Artifact pom) {
                AbstractRepoCommand.this.doWithRepositorySession(system, session, source, manifest,
                        zip, pom, commandLine);
            }

            @Override
            protected ProvenanceInfo getProvenanceInfo() {
                return AbstractRepoCommand.this.getProvenanceInfo(projectRoot, source);
            }
        };

        deployer.deploy(projectRoot, source, artifact);
    }

    protected abstract void doWithRepositorySession(RepositorySystem system,
            RepositorySystemSession session, ArtifactSource source, Manifest manifest, Artifact zip,
            Artifact pom, CommandLine commandLine);

    private ArtifactSource createArtifactSource(File projectRoot) {
        ArtifactSource source = new FileSystemArtifactSource(
                new SimpleFileSystemArtifactSourceIdentifier(projectRoot));
        return ArtifactSourceUtils.filter(source);
    }

    private ProvenanceInfo getProvenanceInfo(File projectRoot, ArtifactSource source) {
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

    private void prepareTargetDirectory(File zipFile) {
        FileUtils.deleteQuietly(zipFile.getParentFile());
        if (!zipFile.getParentFile().exists()) {
            zipFile.getParentFile().mkdirs();
        }
    }
}
