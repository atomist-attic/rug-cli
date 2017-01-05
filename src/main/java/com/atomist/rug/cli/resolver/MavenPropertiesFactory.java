package com.atomist.rug.cli.resolver;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.aether.RepositorySystem;

import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.settings.Settings.RemoteRepository;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.maven.MavenConfiguration;
import com.atomist.rug.resolver.maven.MavenProperties;
import com.atomist.rug.resolver.maven.MavenProperties.Auth;
import com.atomist.rug.resolver.maven.MavenProperties.Repo;

abstract class MavenPropertiesFactory {

    public static MavenProperties create(boolean offline, boolean cacheMetadata) {
        MavenProperties properties = new MavenProperties();
        properties.setOffline(offline);
        properties.setCacheMetadata(cacheMetadata);

        Settings settings = new SettingsReader().read();

        properties.setRepoLocation(settings.getLocalRepository().path());
        properties.setRepos(settings.getRemoteRepositories().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    RemoteRepository r = e.getValue();

                    String url = StringUtils.expandEnvironmentVars(r.getUrl());
                    Repo repo = new Repo();
                    repo.setUrl(url);

                    if (r.getAuthentication() != null) {
                        String username = r.getAuthentication().getUsername();
                        String password = r.getAuthentication().getPassword();
                        Auth auth = new Auth();
                        auth.setUsername(StringUtils.expandEnvironmentVars(username));
                        auth.setPassword(StringUtils.expandEnvironmentVars(password));
                        repo.setAuth(auth);
                    }
                    return repo;
                })));

        return properties;
    }

    public static RepositorySystem repositorySystem() {
        return new MavenConfiguration().repositorySystem();
    }
}
