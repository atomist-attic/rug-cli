package com.atomist.rug.cli.command.utils;

import java.util.List;

import org.eclipse.aether.repository.RemoteRepository;

import com.atomist.rug.resolver.DependencyResolverException;

public class DependencyResolverExceptionProcessor {

    public static String process(DependencyResolverException e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getMessage());

        formatRepositories(e, sb);

        return sb.toString()
                + "\nMake sure all archives and dependencies are available in configured repositories.";
    }

    private static void formatRepositories(DependencyResolverException e, StringBuilder sb) {
        List<RemoteRepository> repositories = e.getRemoteRepositories();
        if (repositories != null && !repositories.isEmpty()) {
            sb.append("\n\nConfigured Repositories:\n");
            repositories.stream().forEach(r -> sb.append("  ").append(r.getId()).append(" (")
                    .append(r.getUrl()).append(")\n"));
        }
    }
}
