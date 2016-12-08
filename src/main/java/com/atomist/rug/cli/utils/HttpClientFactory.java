package com.atomist.rug.cli.utils;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import com.atomist.rug.cli.version.VersionUtils;

public class HttpClientFactory {

    private static void configureProxy(HttpClientBuilder builder, String url) {
        List<Proxy> proxies = ProxySelector.getDefault().select(URI.create(url));
        if (!proxies.isEmpty()) {
            Optional<Proxy> proxy = proxies.stream().filter(p -> p.type().equals(Proxy.Type.HTTP))
                    .findFirst();
            if (proxy.isPresent()) {
                InetSocketAddress address = (InetSocketAddress) proxy.get().address();
                builder.setProxy(new HttpHost(address.getHostName(), address.getPort()));

                try {
                    PasswordAuthentication auth = Authenticator.requestPasswordAuthentication(
                            address.getHostName(), null, address.getPort(),
                            (url.startsWith("https://") ? "https" : "http"),
                            "Credentials for proxy " + proxy, null, new URL(url),
                            Authenticator.RequestorType.PROXY);
                    if (auth != null) {
                        CredentialsProvider credsProvider = new BasicCredentialsProvider();
                        credsProvider.setCredentials(
                                new AuthScope(address.getHostName(), address.getPort()),
                                new UsernamePasswordCredentials(auth.getUserName(),
                                        String.valueOf(auth.getPassword())));
                        builder.setDefaultCredentialsProvider(credsProvider);
                    }
                }
                catch (MalformedURLException e) {
                }
            }
        }
    }

    public static HttpClient createHttpClient(String url) {
        HttpClientBuilder builder = HttpClientBuilder.create()
                .setUserAgent("rug-cli-" + VersionUtils.readVersion().orElse("0.0.0"))
                .useSystemProperties();
        configureProxy(builder, url);
        return builder.build();
    }

}
