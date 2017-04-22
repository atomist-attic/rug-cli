package com.atomist.rug.cli.command.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.config.ConfigureRepositoriesHttpRequest.Repo;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.Settings.Authentication;
import com.atomist.rug.cli.settings.Settings.RemoteRepository;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.settings.SettingsWriter;

public class ConfigureRepositoriesOperations {
    
    private static final Log log = new Log(ConfigureRepositoriesOperations.class);

    public static final String REPO_SERVICE_KEY = "repositories_service_urls";
    public static final List<String> REPO_SERVICE_URL = Arrays
            .asList("https://api.atomist.com/user/team");

    @Command
    public void run(Settings settings) {
        configure(settings);
    }

    private void configure(Settings settings) {
        List<Repo> remoteRepos = new ProgressReportingOperationRunner<List<Repo>>(
                "Configuring team-scoped repositories").run((indicator) -> {
                    return getRepoServiceUrls(settings).stream().map(s -> {
                        indicator.detail(s);
                        return new ConfigureRepositoriesHttpRequest().getForRepos(settings
                                .getConfigValue(Settings.GIHUB_TOKEN_KEY, String.class).get(), s);
                    }).flatMap(List::stream).distinct().collect(Collectors.toList());
                });
        Map<String, RemoteRepository> configuredRepos = settings.getRemoteRepositories();

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Repositories"));

        if (remoteRepos.isEmpty()) {
            log.info(Style.yellow("  No repositories configured"));
        }

        remoteRepos.forEach(r -> {
            RemoteRepository configuredRepo = null;
            Optional<RemoteRepository> repoOption = configuredRepos.values().stream()
                    .filter(cr -> cr.getUrl().equals(r.url())).findFirst();

            if (repoOption.isPresent()) {
                configuredRepo = repoOption.get();
                configuredRepo.setName(r.teamName());
            }
            else {
                configuredRepo = new RemoteRepository();
                configuredRepo.setUrl(r.url());
                configuredRepo.setName(r.teamName().toLowerCase().replace(" ", "-"));
                configuredRepos.put(r.teamId().toLowerCase(), configuredRepo);
            }
            if (r.creds() != null && r.creds().apikey() != null && r.creds().user() != null) {
                Authentication auth = new Authentication();
                auth.setUsername(r.creds().user());
                auth.setPassword(r.creds().apikey());
                configuredRepo.setPublish(true);
                configuredRepo.setAuthentication(auth);
            }
            log.info("  %s (%s)\n  %s%s", Style.yellow(r.teamId()), r.teamName(),
                    Constants.LAST_TREE_NODE, Style.underline(r.url()));
        });

        settings.setRemoteRepositories(configuredRepos);
        SettingsWriter.write(settings, new File(SettingsReader.PATH));

        log.newline();
        log.info(Style.green("Successfully configured team-scoped repositories"));
    }

    private List<String> getRepoServiceUrls(Settings settings) {
        return settings.getConfigValue(REPO_SERVICE_KEY, REPO_SERVICE_URL);
    }
}