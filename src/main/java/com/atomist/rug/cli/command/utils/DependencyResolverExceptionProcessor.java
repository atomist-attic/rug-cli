package com.atomist.rug.cli.command.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

import org.eclipse.aether.repository.RemoteRepository;

import com.atomist.project.ProvenanceInfo;
import com.atomist.project.ProvenanceInfoArtifactSourceReader;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.DependencyResolverException;
import com.atomist.rug.resolver.LocalArtifactDescriptor;
import com.atomist.rug.resolver.maven.DependencyCollectionException;
import com.atomist.rug.resolver.maven.DependencyCollectionException.ErrorType;
import com.atomist.source.ArtifactSource;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;
import com.atomist.source.file.ZipFileArtifactSourceReader;
import com.atomist.source.file.ZipFileInput;

public class DependencyResolverExceptionProcessor {

    public static String process(ArtifactDescriptor artifact, DependencyResolverException e) {
        if (e instanceof DependencyCollectionException) {
            DependencyCollectionException dce = (DependencyCollectionException) e;
            StringBuilder sb = new StringBuilder();

            if (dce.getType() == ErrorType.DEPENDENCY_ERROR) {
                sb.append(dce.getDetailMessage()).append("\n\n");
            }

            if (e instanceof DependencyCollectionException) {
                formatRepositories((DependencyCollectionException) e, sb);
            }

            if (dce.getType() == ErrorType.DEPENDENCY_ERROR) {
                sb.append(String.format(
                        "\nUnable to resolve requested archive %s:%s:%s because of missing dependencies.",
                        artifact.group(), artifact.artifact(), artifact.version()));
            }
            else {
                sb.append(String.format(
                        "\nUnable to resolve requested archive %s:%s:%s.\nPlease verify that specified archive exists in at least on of your configured repositories.",
                        artifact.group(), artifact.artifact(), artifact.version()));
            }

            Optional<ProvenanceInfo> provanceInfo = resolveToProvenanceInfo(artifact);
            if (provanceInfo.isPresent() && provanceInfo.get().repo().isDefined()) {
                String url = getRepositoryUrl(provanceInfo.get());
                sb.append("\nPlease raise an issue at: ").append(url);
            }

            return sb.toString();
        }
        else {
            return e.getMessage();
        }
    }

    private static void formatRepositories(DependencyCollectionException e, StringBuilder sb) {
        List<RemoteRepository> repositories = e.getRemoteRepositories();
        if (repositories != null && !repositories.isEmpty()) {
            sb.append("Configured repositories from ~/.atomist/cli.yml:\n");
            repositories.stream().forEach(r -> sb.append("  ").append(r.getId()).append(" (")
                    .append(r.getUrl()).append(")\n"));
        }
    }

    private static Optional<ProvenanceInfo> resolveToProvenanceInfo(ArtifactDescriptor artifact) {
        if (artifact instanceof LocalArtifactDescriptor) {
            ArtifactSource source = new FileSystemArtifactSource(
                    new SimpleFileSystemArtifactSourceIdentifier(new File(artifact.uri())));
            return new ProvenanceInfoArtifactSourceReader().read(source);
        }
        else {
            File repoFile = new File(new SettingsReader().read().getLocalRepository().path());
            File artifactFile = new File(repoFile, artifact.group().replace(".", File.separator)
                    + File.separator + artifact.artifact() + File.separator + artifact.version()
                    + File.separator + artifact.artifact() + "-" + artifact.version() + ".zip");
            if (artifactFile.exists()) {
                try {
                    ArtifactSource source = ZipFileArtifactSourceReader
                            .fromZipSource(new ZipFileInput(new FileInputStream(artifactFile)));
                    return new ProvenanceInfoArtifactSourceReader().read(source);
                }
                catch (FileNotFoundException e) {
                    // really???, I love checked exceptions
                }
            }
        }
        return Optional.empty();
    }

    private static String getRepositoryUrl(ProvenanceInfo info) {
        if (info.repo().isDefined()) {
            String repo = info.repo().get();
            if (repo.endsWith(".git")) {
                repo = repo.substring(0, repo.length() - 4);
            }
            return String.format("https://github.com/%s/issues", repo);
        }
        return null;
    }
}
