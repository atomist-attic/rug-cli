package com.atomist.rug.cli.command.repo;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.annotation.Validator;
import com.atomist.rug.cli.command.repo.ConfigureOperations.Repo;
import com.atomist.rug.cli.command.repo.LoginOperations.Status;
import com.atomist.rug.cli.command.shell.ShellUtils;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.Settings.Authentication;
import com.atomist.rug.cli.settings.Settings.RemoteRepository;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.settings.SettingsWriter;

public class RepositoriesCommand extends AbstractAnnotationBasedCommand {

    public static final String REPO_SERVICE_KEY = "repositories_service_urls";
    public static final List<String> REPO_SERVICE_URL = Arrays
            .asList("https://api.atomist.com/user/team");

    private static final String BANNER = "The command will create a GitHub Personal Access Token with scope 'read:org'\n"
            + "which you can revoke any time on https://github.com/settings/tokens.  Your\n"
            + "password will not be displayed or stored.  Your sensitive information will not\n"
            + "be sent to Atomist; only to api.github.com.";

    @Validator
    public void validate(@Argument(index = 1, defaultValue = "") String subcommand,
            Settings settings) {
        if ("".equals(subcommand)) {
            throw new CommandException("No SUBCOMMAND provided.", "repositories");
        }
        if (!"login".equals(subcommand) && !"configure".equals(subcommand)) {
            throw new CommandException(
                    "Invalid SUBCOMMAND provided. Please specify login or configure.",
                    "repositories");
        }
        if ("configure".equals(subcommand)
                && !settings.getConfigValue(Settings.GIHUB_TOKEN_KEY, String.class).isPresent()) {
            throw new CommandException(
                    "No token configured. Please run repositories login before running this command.",
                    "repositories configure");
        }
    }

    @Command
    public void run(@Argument(index = 1, defaultValue = "") String subcommand,
            @Option("username") String username, @Option("mfa-code") String code,
            Settings settings) {
        switch (subcommand) {
        case "login":
            login(username, code, settings);
            break;
        case "configure":
            configure(settings);
        }
    }

    private void configure(Settings settings) {
        List<Repo> remoteRepos = new ProgressReportingOperationRunner<List<Repo>>(
                "Configuring team-scoped repositories").run((indicator) -> {
                    return getRepoServiceUrls(settings).stream().map(s -> {
                        indicator.report("  Querying " + s);
                        return new ConfigureOperations().getForRepos(settings
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

    private void login(String username, String code, Settings settings) {
        printBanner();

        LineReader reader = ShellUtils.lineReader(null);
        try {
            if (username == null) {
                username = reader.readLine(getPrompt("Username"));
            }
            String password = reader.readLine(getPrompt("Password"), new Character('*'));
            postForTokenAndHandleResponse(username, password, code, settings, reader);
        }
        catch (EndOfFileException | UserInterruptException e) {
            log.error("Canceled!");
        }
        finally {
            ShellUtils.shutdown(reader);
        }
    }

    private void printBanner() {
        log.newline();
        log.info("The Rug CLI needs your GitHub login to identify you.");
        log.newline();

        String banner = BANNER.replace("https://github.com/settings/tokens",
                Style.underline("https://github.com/settings/tokens"));
        banner = banner.replace("sensitive information will not\nbe sent to Atomist",
                Style.bold("sensitive information will not\nbe sent to Atomist"));
        log.info(banner);

        log.newline();
    }

    private void postForTokenAndHandleResponse(String username, String password, String code,
            Settings settings, LineReader reader) {
        Status status = new LoginOperations().postForToken(username, password, code, settings);

        if (status == Status.OK) {
            log.newline();
            log.info(Style.green(
                    "Successfully logged in to GitHub and stored token in ~/.atomist/cli.yml"));
        }
        else if (status == Status.BAD_CREDENTIALS) {
            throw new CommandException(
                    "Provided credentials are invalid. Please try again with correct credentials.",
                    "login");
        }
        else if (status == Status.MFA_REQUIRED) {
            log.newline();
            log.info("  Please provide a MFA code");
            code = reader.readLine(getPrompt("MFA code"));
            postForTokenAndHandleResponse(username, password, code, settings, reader);
        }
    }

    private String getPrompt(String name) {
        return String.format("  %s %s : ", Style.cyan(Constants.DIVIDER), Style.yellow(name));
    }

    private List<String> getRepoServiceUrls(Settings settings) {
        return settings.getConfigValue(REPO_SERVICE_KEY, REPO_SERVICE_URL);
    }
}