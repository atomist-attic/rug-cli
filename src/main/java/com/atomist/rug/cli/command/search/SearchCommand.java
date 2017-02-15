package com.atomist.rug.cli.command.search;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.springframework.util.StringUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.utils.HttpClientFactory;
import com.atomist.rug.cli.version.VersionUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unused")
public class SearchCommand extends AbstractAnnotationBasedCommand {

    private ObjectMapper mapper = new ObjectMapper();

    @Command
    public void run(Settings settings, @Argument(index = 1) String search,
            @Option("tag") Properties tags, @Option("type") String type) {

        if (settings.getCatalogs().getUrls().isEmpty()) {
            throw new CommandException("No catalog endpoints configured in cli.yml.");
        }

        Map<String, List<Operation>> operations = new ProgressReportingOperationRunner<Map<String, List<Operation>>>(
                "Searching catalogs").run(indicator -> {
                    List<Operation> results = settings.getCatalogs().getUrls().stream().map(u -> {
                        indicator.report("  Searching " + u);
                        return collectResults(u, search, type, tags);
                    }).flatMap(List::stream).collect(Collectors.toList());

                    return results.stream()
                            .collect(Collectors.groupingBy(o -> o.getArchive().key()));

                });

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Remote Archives") + " ("
                + operations.size() + " "
                + com.atomist.rug.cli.utils.StringUtils.puralize("archive", operations.keySet())
                + " found)");

        if (operations.isEmpty()) {
            log.info(Style.yellow("  No matching archives found"));
            log.newline();
        }
        else {
            operations.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
                    .forEach(a -> printArchive(a.getValue()));
            log.info("\nFor more information on specific archive version, run:\n"
                    + "  %s describe archive ARCHIVE -a VERSION", Constants.COMMAND);
        }
    }

    private void printArchive(List<Operation> operations) {
        Archive archive = operations.get(0).getArchive();
        log.info("  %s (%s)", Style.yellow("%s:%s", archive.getGroup(), archive.getArtifact()),
                archive.getVersion().getValue());
    }

    private List<Operation> collectResults(String endpoint, String search, String type,
            Properties tags) {

        if (!endpoint.endsWith(Constants.CATALOG_PATH)) {
            if (!endpoint.endsWith("/")) {
                endpoint = endpoint + "/";
            }
            endpoint = endpoint + Constants.CATALOG_PATH;
        }

        HttpClient client = HttpClientFactory.createHttpClient(endpoint,
                Constants.ARTIFACT + "-" + VersionUtils.readVersion().orElse("0.0.0"));
        HttpPost post = new HttpPost(endpoint);

        StringEntity requestEntity = new StringEntity(getSearchQuery(search, type, tags),
                ContentType.APPLICATION_JSON);

        post.setEntity(requestEntity);
        try {
            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return mapper.readValue(response.getEntity().getContent(), Operations.class)
                        .getOperations();
            }
        }
        catch (ClientProtocolException e) {
            new CommandException("Client error occurred searching online catalog", e);
        }
        catch (IOException e) {
            new CommandException("IO error occurred searching online catalog", e);
        }
        return Collections.emptyList();
    }

    private String getSearchQuery(String search, String type, Properties tags) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"queries\": [{ ");
        sb.append("\"archive\": { \"scope\": \"public\" }");
        if (search != null && search.length() > 0) {
            sb.append(String.format(", \"search\": \"%s\"", search));
        }
        if (tags.size() > 0) {
            sb.append(String.format(", \"tags\": [%s]", toCommaSeperatedList(tags)));
        }
        if (type != null) {
            sb.append(String.format(", \"operation\": { \"type\": \"%s\" }", type));
        }
        sb.append(" }]}");
        return sb.toString();
    }

    private String toCommaSeperatedList(Properties tags) {
        if (tags.isEmpty()) {
            return "";
        }
        else {
            return StringUtils.collectionToCommaDelimitedString(
                    tags.keySet().stream().map(k -> "\"" + k + "\"").collect(Collectors.toList()));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Operations {
        private List<Operation> operations;

        public List<Operation> getOperations() {
            return operations;
        }

        public void setOperations(List<Operation> operations) {
            this.operations = operations;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Operation {

        private String type;
        private String name;

        private Archive archive;

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public Archive getArchive() {
            return archive;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Archive {

        private String group;
        private String artifact;

        private Version version;

        public String getGroup() {
            return group;
        }

        public String getArtifact() {
            return artifact;
        }

        public Version getVersion() {
            return version;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public void setArtifact(String artifact) {
            this.artifact = artifact;
        }

        public void setVersion(Version version) {
            this.version = version;
        }

        public String key() {
            return String.format("%s:%s:%s", group, artifact, version.value);
        }
    }

    private static class Version {
        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}