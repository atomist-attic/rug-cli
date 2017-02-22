package com.atomist.rug.cli.command.repo;

import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.utils.HttpClientFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

public class ConfigureOperations {

    public List<Repo> getForRepos(String token) {

        HttpClient client = HttpClientFactory.httpClient(Constants.REPO_URL);
        HttpGet get = new HttpGet(Constants.REPO_URL);
        HttpClientFactory.authorizationHeader(get, token);

        HttpResponse response = new ProgressReportingOperationRunner<HttpResponse>(
                "Configuring team-scoped repositories").run((indicator) -> {
                    return HttpClientFactory.execute(client, get);
                });

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return HttpClientFactory.read(response, new TypeReference<List<Repo>>() {
            });
        }
        else {
            throw new CommandException("Failed to configure team-scoped repositories.",
                    "repositories configure");
        }
    }

    public static class Repo {
        @JsonProperty("team-id")
        private String teamId;
        @JsonProperty
        private String url;
        @JsonProperty("team-name")
        private String teamName;
        @JsonProperty
        private Creds creds;

        public String teamId() {
            return teamId.toLowerCase();
        }

        public String url() {
            return url;
        }

        public Creds creds() {
            return creds;
        }
        
        public String teamName() {
            return teamName;
        }
    }

    public static class Creds {
        @JsonProperty
        private String apikey;
        @JsonProperty
        private String user;
        @JsonProperty
        private String password;

        public String apikey() {
            return apikey;
        }

        public String user() {
            return user;
        }
    }
}
