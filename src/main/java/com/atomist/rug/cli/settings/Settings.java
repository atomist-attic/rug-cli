package com.atomist.rug.cli.settings;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.atomist.rug.cli.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_EMPTY)
public class Settings {

    @JsonProperty("default")
    private Defaults defaults = new Defaults();

    @JsonProperty("local-repository")
    private LocalRepository localRepository = new LocalRepository();

    @JsonProperty("remote-repositories")
    private Map<String, RemoteRepository> remoteRepositories = new HashMap<>();

    public Defaults getDefaults() {
        return defaults;
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
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
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
