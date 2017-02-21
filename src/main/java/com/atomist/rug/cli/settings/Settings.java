package com.atomist.rug.cli.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(Include.NON_EMPTY)
public class Settings {

    @JsonProperty("catalogs")
    private Catalogs catalogs = new Catalogs();

    @JsonProperty("default")
    private Defaults defaults = new Defaults();

    @JsonProperty("local-repository")
    private LocalRepository localRepository = new LocalRepository();

    @JsonProperty("remote-repositories")
    private Map<String, RemoteRepository> remoteRepositories = new HashMap<>();

    @JsonProperty("config")
    private Map<String, Object> config = new HashMap<>();

    @JsonProperty("token")
    private String token;

    public String getToken() {
        return token;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getConfigValue(String key, Class<T> cls) {
        return Optional.ofNullable((T) config.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, T defaultValue) {
        if (config.containsKey(key)) {
            return (T) config.get(key);
        }
        else {
            return (T) defaultValue;
        }
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public Catalogs getCatalogs() {
        return catalogs;
    }

    public LocalRepository getLocalRepository() {
        return localRepository;
    }

    public Map<String, RemoteRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    public void override(Settings project) {
        if (project.getLocalRepository() != null
                && project.getLocalRepository().getPath() != null) {
            localRepository.setPath(project.getLocalRepository().getPath());
        }
        remoteRepositories.putAll(project.getRemoteRepositories());
        if (project.getDefaults() != null) {
            if (project.getDefaults().getGroup() != null) {
                defaults.setGroup(project.getDefaults().getGroup());
            }
            if (project.getDefaults().getArtifact() != null) {
                defaults.setArtifact(project.getDefaults().getArtifact());
            }
            if (project.getDefaults().getVersion() != null) {
                defaults.setVersion(project.getDefaults().getVersion());
            }
        }
        if (project.getToken() != null) {
            token = project.getToken();
        }
        if (project.getConfig() != null) {
            config.putAll(project.getConfig());
        }
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public void setConfigValue(String key, String value) {
        config.put(key, value);
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    public void setCatalogs(Catalogs catalogs) {
        this.catalogs = catalogs;
    }

    public void setLocalRepository(LocalRepository localRepository) {
        this.localRepository = localRepository;
    }

    public void setRemoteRepositories(Map<String, RemoteRepository> remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }

    @JsonInclude(Include.NON_EMPTY)
    public static class Authentication {

        private String password;
        private String username;

        public String getPassword() {
            return password;
        }

        public String getUsername() {
            return username;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    @JsonInclude(Include.NON_EMPTY)
    public static class Defaults {

        private String artifact;
        private String group;
        private String version;

        public String getArtifact() {
            return artifact;
        }

        public String getGroup() {
            return group;
        }

        public String getVersion() {
            return version;
        }

        public void setArtifact(String artifact) {
            this.artifact = artifact;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public void setVersion(String version) {
            this.version = version;
        }

    }

    @JsonInclude(Include.NON_EMPTY)
    public static class LocalRepository {

        private String path;

        public String getPath() {
            return path;
        }

        public String path() {
            return StringUtils.expandEnvironmentVars(path);
        }

        public void setPath(String path) {
            this.path = path.replace("/", File.separator);
        }
    }

    @JsonInclude(Include.NON_EMPTY)
    public static class Catalogs {

        private List<String> urls = new ArrayList<>();

        public void addUrl(String url) {
            urls.add(url);
        }

        @JsonValue
        public List<String> getUrls() {
            if (urls == null || urls.size() == 0) {
                return Collections.singletonList(Constants.CATALOG_URL);
            }
            else {
                return urls;
            }
        }
    }

    @JsonInclude(Include.NON_EMPTY)
    public static class RemoteRepository {

        private Authentication authentication;

        private boolean publish = false;

        private String url;

        public Authentication getAuthentication() {
            return authentication;
        }

        public String getUrl() {
            return url;
        }

        public boolean isPublish() {
            return publish;
        }

        public void setAuthentication(Authentication authentication) {
            this.authentication = authentication;
        }

        public void setPublish(boolean publish) {
            this.publish = publish;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
