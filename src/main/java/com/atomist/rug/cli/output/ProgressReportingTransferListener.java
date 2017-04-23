package com.atomist.rug.cli.output;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferEvent.RequestType;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.settings.Settings.RemoteRepository;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.StringUtils;

public class ProgressReportingTransferListener extends AbstractTransferListener {

    private boolean reportTitle = true;
    private ProgressReporter indicator;

    private Map<String, String> repositories;

    public ProgressReportingTransferListener(ProgressReporter indicator, boolean reportTitle) {
        this(SettingsReader.read().getRemoteRepositories(), indicator);
        this.reportTitle = reportTitle;
    }

    public ProgressReportingTransferListener(ProgressReporter indicator) {
        this(SettingsReader.read().getRemoteRepositories(), indicator);
    }

    public ProgressReportingTransferListener(Map<String, RemoteRepository> repositories,
            ProgressReporter indicator) {
        this.indicator = indicator;
        this.repositories = repositories.entrySet().stream().collect(
                Collectors.toMap(e -> sanitizeUrl(e.getValue().getUrl()), Map.Entry::getKey));

    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        if (CommandLineOptions.hasOption("V")) {
            report(event);
        }
    }

    @Override
    public void transferFailed(TransferEvent event) {
        if (CommandLineOptions.hasOption("V")) {
            report(event);
        }
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        if (CommandLineOptions.hasOption("V") || CommandLineOptions.hasOption("q")) {
            report(event);
        }
        else {
            reportDetail(event);
        }
    }

    private void reportDetail(TransferEvent event) {
        String[] parts = event.getResource().getResourceName().split("/");
        String repo = repositories.getOrDefault(
                sanitizeUrl(event.getResource().getRepositoryUrl()),
                event.getResource().getRepositoryUrl());
        if (!event.getResource().getResourceName().endsWith("maven-metadata.xml")) {
            // [com, atomist, rug-cli-root, 1.0.0, rug-cli-root-1.0.0.pom]
            String version = null;
            String artifact = null;
            String group = null;
            String ext = null;
            for (int i = parts.length - 1; i >= 0; i--) {
                if (ext == null) {
                    String name = parts[i];
                    if (name.endsWith(".jar.asc")) {
                        ext = "jar.asc";
                    }
                    else if (name.endsWith(".pom.asc")) {
                        ext = "pom.asc";
                    }
                    else if (name.endsWith(".jar")) {
                        ext = "jar";
                    }
                    else if (name.endsWith(".pom")) {
                        ext = "pom";
                    }
                    else if (name.endsWith(".json")) {
                        ext = "json";
                    }
                    else if (name.endsWith(".zip")) {
                        ext = "json";
                    }
                    else {
                        ext = name;
                    }
                }
                else if (version == null) {
                    version = parts[i];
                }
                else if (artifact == null) {
                    artifact = parts[i];
                }
                else if (group == null) {
                    group = parts[i];
                }
                else {
                    group = parts[i] + "." + group;
                }
            }
            ProgressReporterUtils.detail(group + ":" + artifact + " (" + version + Constants.DOT
                    + ext + Constants.DOT + repo + ")");
        }
        else {
            // [com, atomist, rug, rug-function-http, maven-metadata.xml]
            String artifact = null;
            String group = null;
            for (int i = parts.length - 2; i >= 0; i--) {
                if (artifact == null) {
                    artifact = parts[i];
                }
                else if (group == null) {
                    group = parts[i];
                }
                else {
                    group = parts[i] + "." + group;
                }
            }
            ProgressReporterUtils
                    .detail(group + ":" + artifact + " (metadata" + Constants.DOT + repo + ")");
        }
    }

    private synchronized void report(TransferEvent event) {
        String message = messageFrom(event);
        if (reportTitle) {
            indicator.report("Processing dependencies");
            reportTitle = false;
        }
        indicator.report(message);
    }

    private String messageFrom(TransferEvent event) {
        StringBuilder message = new StringBuilder();
        if (event.getRequestType().equals(RequestType.PUT)) {
            message.append("  Uploading ");
        }
        else {
            message.append("  Downloading ");
        }
        message.append(event.getResource().getResourceName());
        if (event.getRequestType().equals(RequestType.PUT)) {
            message.append(" ").append(Constants.DIVIDER).append(" ");
        }
        else {
            message.append(" ").append(Constants.REDIVID).append(" ");
        }
        message.append(
                repositories.getOrDefault(sanitizeUrl(event.getResource().getRepositoryUrl()),
                        event.getResource().getRepositoryUrl()));
        // only print size if > 0
        if (event.getResource().getContentLength() > 0) {
            message.append(" (");
            message.append(FileUtils.byteCountToDisplaySize(event.getResource().getContentLength())
                    .toLowerCase());
            message.append(")");
        }
        message.append(" ");
        String eventMsg = event.getType().toString().toLowerCase();
        switch (event.getType()) {
        case SUCCEEDED:
            message.append(Style.green(eventMsg));
            break;
        case INITIATED:
        case PROGRESSED:
        case STARTED:
            message.append(eventMsg);
            break;
        case CORRUPTED:
        case FAILED:
            message.append(Style.red(eventMsg));
            break;

        }
        return message.toString();
    }

    private String sanitizeUrl(String url) {
        url = StringUtils.expandEnvironmentVars(url);
        if (url.endsWith("/")) {
            return URI.create(url).getPath();
        }
        else {
            return URI.create(url + "/").getPath();
        }
    }
}
