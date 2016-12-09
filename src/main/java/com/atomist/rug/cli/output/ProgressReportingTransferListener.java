package com.atomist.rug.cli.output;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

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

    private ProgressReporter indicator;

    private Map<String, String> repositories;

    public ProgressReportingTransferListener(ProgressReporter indicator) {
        this(new SettingsReader().read().getRemoteRepositories(), indicator);
    }

    public ProgressReportingTransferListener(Map<String, RemoteRepository> repositories,
            ProgressReporter indicator) {
        this.indicator = indicator;
        this.repositories = repositories.entrySet().stream().collect(
                Collectors.toMap(e -> sanitizeUrl(e.getValue().getUrl()), Map.Entry::getKey));

    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        if (CommandLineOptions.hasOption("X")) {
            indicator.report(messageFrom(event));
        }
    }

    @Override
    public void transferFailed(TransferEvent event) {
        if (CommandLineOptions.hasOption("X")) {
            indicator.report(messageFrom(event));
        }
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        indicator.report(messageFrom(event));
    }

    private String messageFrom(TransferEvent event) {
        StringBuilder message = new StringBuilder();
        if (event.getRequestType().equals(RequestType.PUT)) {
            message.append("Uploading ");
        }
        else {
            message.append("Downloading ");
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
        message.append(" (");
        message.append((event.getResource().getContentLength() / 1024));
        message.append("kb) ");

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
