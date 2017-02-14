package com.atomist.rug.cli.command.login;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.shell.ShellUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.settings.SettingsWriter;
import com.atomist.rug.cli.utils.HttpClientFactory;
import com.atomist.rug.cli.version.VersionUtils;
import com.jayway.jsonpath.JsonPath;

public class LoginCommand extends AbstractAnnotationBasedCommand {

    private static final String GITHUB_ENDPOINT = "https://api.github.com/authorizations";

    private static final String BODY = "{\n" + "  \"scopes\": [\n" + "    \"read:org\"\n" + "  ],\n"
            + "  \"note\": \"Rug CLI on %s\",\n"
            + "  \"note_url\": \"https://github.com/atomist/rug-cli\",\n"
            + "  \"fingerprint\": \"%s\"\n" + "}";

    private static final String BANNER = "The command will create a GitHub Personal Access Token with scope 'read:org' which you can revoke\n"
            + "any time on https://github.com/settings/tokens.  Your password will not be displayed or stored.\n"
            + "Your sensitve information will not be sent to Atomist; only to api.github.com.";

    @Command
    public void run(@Option("username") String username, @Option("mfa-code") String code,
            Settings settings) {

        printBanner();

        try {
            LineReader reader = ShellUtils.lineReader(null);
            if (username == null) {
                username = reader.readLine(getPrompt("Username"));
            }
            String password = reader.readLine(getPrompt("Password"), new Character('*'));
            postAndHandleResponse(username, password, code, settings, reader);
        }
        catch (EndOfFileException | UserInterruptException e) {
            log.error("Canceled!");
        }
    }

    private void printBanner() {
        log.newline();
        log.info("The Rug CLI needs your GitHub login to identify you.");
        log.newline();

        String banner = BANNER.replace("https://github.com/settings/tokens",
                Style.underline("https://github.com/settings/tokens"));
        banner = banner.replace("sensitve information will not be sent to Atomist",
                Style.bold("sensitve information will not be sent to Atomist"));
        log.info(banner);

        log.newline();
    }

    private void postAndHandleResponse(String username, String password, String code,
            Settings settings, LineReader reader) {
        Status status = post(username, password, code, settings);

        if (status == Status.OK) {
            log.newline();
            log.info(Style.green("Successfully logged in to GitHub and stored token in ~/.atomist/cli.yml"));
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
            postAndHandleResponse(username, password, code, settings, reader);
        }
    }

    private Status post(String username, String password, String code, Settings settings) {
        HttpClient client = HttpClientFactory.createHttpClient(GITHUB_ENDPOINT,
                "rug-cli-" + VersionUtils.readVersion().orElse("0.0.0"));
        HttpPost post = new HttpPost(GITHUB_ENDPOINT);

        if (code != null) {
            post.addHeader("X-GitHub-OTP", code);
        }

        if (username != null && password != null) {
            post.addHeader("Authorization", encodeBaseAuthHeader(username, password));
        }

        try {
            StringEntity entity = new StringEntity(String.format(BODY,
                    InetAddress.getLocalHost().getHostName(), UUID.randomUUID().toString()));
            post.setEntity(entity);

            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                String token = JsonPath.read(response.getEntity().getContent(), "$.token");
                settings.setToken(token);
                SettingsWriter.write(settings, new File(SettingsReader.PATH));
                return Status.OK;
            }
            else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                String message = JsonPath.read(response.getEntity().getContent(), "$.message");
                // "Bad credentials"
                if ("Bad credentials".equals(message)
                        || "Requires authentication".equals(message)) {
                    return Status.BAD_CREDENTIALS;
                }
                // "Must specify two-factor authentication OTP code."
                else if ("Must specify two-factor authentication OTP code.".equals(message)) {
                    if (code != null) {
                        return Status.BAD_CREDENTIALS;
                    }
                    else {
                        return Status.MFA_REQUIRED;
                    }
                }
            }
            return Status.UNKOWN;
        }
        catch (UnsupportedEncodingException e) {
            throw new CommandException("Error occured creating GitHub access token", e);
        }
        catch (UnknownHostException e) {
            throw new CommandException("Error occured creating GitHub access token", e);
        }
        catch (ClientProtocolException e) {
            throw new CommandException("Error occured creating GitHub access token", e);
        }
        catch (IOException e) {
            throw new CommandException("Error occured creating GitHub access token", e);
        }
    }

    private static String encodeBaseAuthHeader(String username, String password) {
        String pair = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes());
    }

    private String getPrompt(String name) {
        return String.format("  %s %s : ", Style.cyan(Constants.DIVIDER), Style.yellow(name));
    }

    private enum Status {
        OK, MFA_REQUIRED, BAD_CREDENTIALS, UNKOWN
    }
}