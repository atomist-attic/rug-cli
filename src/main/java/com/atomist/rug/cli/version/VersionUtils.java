package com.atomist.rug.cli.version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

import com.atomist.rug.cli.utils.HttpClientFactory;

public abstract class VersionUtils {

    private static final String KEY_NAME = "last_checked";
    private static final Preferences prefs = Preferences.userNodeForPackage(VersionUtils.class);
    private static final long TIMEOUT = 1000 * 60 * 60;

    private static final String URL = "https://static.atomist.com/Formula/rug-cli.rb";

    private static final String VERSION_PATTERN_STRING = "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(-(0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(\\.(0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?(\\+[0-9a-zA-Z-]+(\\.[0-9a-zA-Z-]+)*)?";

    public static Optional<String> newerVersion() {
        try {
            VersionScheme scheme = new GenericVersionScheme();
            Version runningVersion = scheme.parseVersion(readVersion().orElse("0.0.0"));
            Version onlineVersion = scheme.parseVersion(readOnlineVersion().orElse("0.0.0"));
            return (onlineVersion.compareTo(runningVersion) > 0
                    ? Optional.of(onlineVersion.toString()) : Optional.empty());
        }
        catch (InvalidVersionSpecificationException e) {
        }
        return Optional.empty();
    }

    @SuppressWarnings("deprecation")
    public static Optional<String> readOnlineVersion() {
        String version = null;

        if (isOutdated()) {
            HttpClient httpClient = HttpClientFactory.createHttpClient(URL);
            HttpGet httpget = new HttpGet(URL);
            try {
                HttpResponse response = httpClient.execute(httpget);

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    HttpEntity entity = response.getEntity();
                    try (InputStream is = entity.getContent()) {
                        Pattern pattern = Pattern.compile(VERSION_PATTERN_STRING);
                        String content = IOUtils.toString(is);
                        Matcher matcher = pattern.matcher(content);
                        if (matcher.find()) {
                            version = matcher.group(0);
                        }
                    }
                }
            } catch (IOException e) {
            }
            finally {
                updateLastChecked();
            }
        }
        return Optional.ofNullable(version);
    }

    public static Optional<String> readVersion() {
        return Optional.ofNullable(readVersionInformation().version());
    }

    public static VersionInformation readVersionInformation() {
        Properties properties = new Properties();
        readFileIntoProperties("META-INF/maven/com.atomist/rug-cli/pom.properties", properties);
        readFileIntoProperties("git-rug-cli.properties", properties);

        String repo = properties.getProperty("git.remote.origin.url", "n/a");
        if (repo != null) {
            repo = repo.replace("git@github.com\\:", "");
            repo = repo.replace("https://github.com/", "");
            properties.put("git.remote.origin.url", repo);
        }

        if (repo != null && repo.indexOf(':') >= 0) {
            repo = repo.substring(repo.indexOf(':') + 1);
        }

        String version = properties.getProperty("version", "n/a");
        String sha = properties.getProperty("git.commit.id.abbrev", "n/a");
        String date = properties.getProperty("git.commit.time", "n/a");

        return new VersionInformation("rug", version, repo, version, sha, date);
    }

    private static boolean isOutdated() {
        long lastCheck = prefs.getLong(KEY_NAME, System.currentTimeMillis());
        return ((System.currentTimeMillis() - lastCheck) > TIMEOUT);
    }

    private static void readFileIntoProperties(String fileName, Properties properties) {
        try (InputStream is = VersionUtils.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is != null) {
                properties.load(is);
            }
        }
        catch (IOException e) {
            // If that's happens so what. We move on without version information
        }
    }

    private static void updateLastChecked() {
        try {
            prefs.putLong(KEY_NAME, System.currentTimeMillis());
            prefs.flush();
            prefs.sync();
        }
        catch (BackingStoreException e) {
        }
    }

    public static class VersionInformation {

        private final String id;

        private final String repo;
        private final String branch;
        private final String sha;
        private final String date;

        private final String version;

        public VersionInformation(String id, String version) {
            this(id, version, null, null, null, null);
        }

        public VersionInformation(String id, String version, String repo, String branch, String sha,
                String date) {
            this.id = id;
            this.version = version;
            this.repo = repo;
            this.branch = branch;
            this.sha = sha;
            this.date = date;
        }

        public String id() {
            return id;
        }

        public String repo() {
            return repo;
        }

        public String branch() {
            return branch;
        }

        public String sha() {
            return sha;
        }

        public String date() {
            return date;
        }

        public String version() {
            return version;
        }
    }
}
