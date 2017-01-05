package com.atomist.rug.cli.command.publish;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.repo.AbstractRepositoryCommand;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.ProgressReportingTransferListener;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.Settings.Authentication;
import com.atomist.rug.cli.settings.Settings.RemoteRepository;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.tree.ArtifactSourceTreeCreator;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.manifest.Manifest;
import com.atomist.source.ArtifactSource;

public class PublishCommand extends AbstractRepositoryCommand {

    private Log log = new Log(PublishCommand.class);

    protected void doWithRepositorySession(RepositorySystem system, RepositorySystemSession session,
            ArtifactSource source, Manifest manifest, Artifact zip, Artifact pom, Artifact metadata,
            CommandLine commandLine) {

        org.eclipse.aether.repository.RemoteRepository deployRepository = getDeployRepository(
                commandLine.getOptionValue("i"));

        String artifactUrl = new ProgressReportingOperationRunner<String>(
                "Publishing archive into remote repository").run(indicator -> {
                    String[] url = new String[1];
                    ((DefaultRepositorySystemSession) session).setTransferListener(
                            new ProgressReportingTransferListener(indicator, false) {

                                @Override
                                public void transferSucceeded(TransferEvent event) {
                                    super.transferSucceeded(event);
                                    if (event.getResource().getResourceName().endsWith(".zip")) {
                                        url[0] = event.getResource().getRepositoryUrl()
                                                + event.getResource().getResourceName();
                                    }
                                }
                            });

                    DeployRequest deployRequest = new DeployRequest();
                    deployRequest.addArtifact(zip).addArtifact(pom).addArtifact(metadata);
                    deployRequest.setRepository(deployRepository);

                    system.deploy(session, deployRequest);

                    return url[0];
                });

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Archive"));
        log.info("  %s (%s in %s files)", Style.underline(FileUtils.relativize(zip.getFile())),
                FileUtils.sizeOf(zip.getFile()), source.allFiles().size());
        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Contents"));
        ArtifactSourceTreeCreator.visitTree(source, new LogVisitor(log));
        log.newline();
        log.info(Style.green("Successfully published archive for %s:%s:%s to\n  %s",
                manifest.group(), manifest.artifact(), manifest.version(), artifactUrl));
    }

    private org.eclipse.aether.repository.RemoteRepository getDeployRepository(String repoId) {
        Settings settings = new SettingsReader().read();
        Map<String, RemoteRepository> deployRepositories = settings.getRemoteRepositories()
                .entrySet().stream().filter(e -> e.getValue().isPublish())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        if (repoId != null) {
            if (deployRepositories.containsKey(repoId)) {
                return toRepository(repoId, deployRepositories.get(repoId));
            }
            else {
                throw new CommandException(String.format(
                        "Specified repository with id %s doesn't exist or is not enabled for publishing. Please review your ~/.atomist/cli.yml",
                        repoId));
            }
        }

        if (deployRepositories.size() > 1) {
            throw new CommandException(
                    "More than one repository enabled for publishing. Please review your ~/.atomist/cli.yml");
        }
        else if (deployRepositories.size() == 0) {
            throw new CommandException(
                    "No repository enabled for publishing. Please review your ~/.atomist/cli.yml");
        }

        Entry<String, RemoteRepository> remoteRepository = deployRepositories.entrySet().stream()
                .findFirst().get();
        return toRepository(remoteRepository.getKey(), remoteRepository.getValue());
    }

    private org.eclipse.aether.repository.RemoteRepository toRepository(String id,
            RemoteRepository remoteRepository) {
        org.eclipse.aether.repository.RemoteRepository.Builder builder = new org.eclipse.aether.repository.RemoteRepository.Builder(
                id, "default", StringUtils.expandEnvironmentVars(remoteRepository.getUrl()));

        if (remoteRepository.getAuthentication() != null) {
            Authentication auth = remoteRepository.getAuthentication();
            builder.setAuthentication(new AuthenticationBuilder()
                    .addUsername(StringUtils.expandEnvironmentVars(auth.getUsername()))
                    .addPassword(StringUtils.expandEnvironmentVars(auth.getPassword())).build())
                    .build();
        }

        return builder.build();

    }

}
