package com.atomist.rug.cli.command.utils;

import java.util.List;

import org.eclipse.aether.repository.RemoteRepository;

import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.DependencyResolverException;
import com.atomist.rug.resolver.maven.DependencyCollectionException;
import com.atomist.rug.resolver.maven.DependencyCollectionException.ErrorType;

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
            repositories.forEach(r -> sb.append("  ").append(r.getId()).append(" (")
                    .append(r.getUrl()).append(")\n"));
        }
    }
}
