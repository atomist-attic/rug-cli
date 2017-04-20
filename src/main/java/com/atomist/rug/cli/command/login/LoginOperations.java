package com.atomist.rug.cli.command.login;

import java.io.File;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.settings.SettingsWriter;
import com.atomist.rug.cli.utils.HttpClientFactory;

public class LoginOperations {

    private static final String GITHUB_ENDPOINT = "https://api.github.com/authorizations";

    private static final String BODY = "{\n" + "  \"scopes\": [\n" + "    \"read:org\"\n" + "  ],\n"
            + "  \"note\": \"Rug CLI on %s\",\n"
            + "  \"note_url\": \"https://github.com/atomist/rug-cli\",\n"
            + "  \"fingerprint\": \"%s\"\n" + "}";

    public Status postForToken(String username, String password, String code, Settings settings) {
        HttpClient client = HttpClientFactory.httpClient(GITHUB_ENDPOINT);
        HttpPost post = new HttpPost(GITHUB_ENDPOINT);
        HttpClientFactory.authorizationHeader(post, username, password);
        HttpClientFactory.header(post, "X-GitHub-OTP", code);
        HttpClientFactory.body(post,
                String.format(BODY, Constants.hostName(), UUID.randomUUID().toString()));

        HttpResponse response = HttpClientFactory.execute(client, post);

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
            String token = HttpClientFactory.jsonValue(response, "$.token").orElse(null);
            settings.setConfigValue(Settings.GIHUB_TOKEN_KEY, token);
            SettingsWriter.write(settings, new File(SettingsReader.PATH));
            return Status.OK;
        }
        else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            String message = HttpClientFactory.jsonValue(response, "$.message").orElse(null);

            // "Bad credentials"
            if ("Bad credentials".equals(message) || "Requires authentication".equals(message)) {
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

    public enum Status {
        OK, MFA_REQUIRED, BAD_CREDENTIALS, UNKOWN
    }
}
