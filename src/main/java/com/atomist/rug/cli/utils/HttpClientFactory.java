package com.atomist.rug.cli.utils;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.RunnerException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

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

    public static HttpClient httpClient(String url) {
        HttpClientBuilder builder = HttpClientBuilder.create().setUserAgent(Constants.httpClient())
                .useSystemProperties();
        configureProxy(builder, url);
        return builder.build();
    }

    public static void header(HttpMessage msg, String key, String value) {
        if (value != null) {
            msg.addHeader(key, value);
        }
    }

    public static void authorizationHeader(HttpMessage msg, String token) {
        if (token != null) {
            msg.addHeader("Authorization", "Bearer " + token);
        }
    }

    public static void authorizationHeader(HttpMessage msg, String username, String password) {
        if (username != null && password != null) {
            msg.addHeader("Authorization", encodeBaseAuthHeader(username, password));
        }
    }
    
    public static HttpEntity body(HttpEntityEnclosingRequest msg, String body) {
        StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
        msg.setEntity(entity);
        return entity;
    }

    private static String encodeBaseAuthHeader(String username, String password) {
        String pair = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes());
    }

    public static HttpResponse execute(HttpClient client, HttpUriRequest request) {
        try {
            return client.execute(request);
        }
        catch (IOException e) {
            throw new RunnerException("Error occured executing http request", e);
        }
    }

    public static Optional<String> jsonValue(HttpResponse response, String jsonPath) {
        try {
            return Optional.ofNullable(JsonPath.read(response.getEntity().getContent(), jsonPath));
        }
        catch (UnsupportedOperationException | IOException e) {
        }
        return Optional.empty();
    }
    
    public static <T> Optional<T> executeAndRead(HttpClient client, HttpUriRequest request, TypeReference<T> ref) {
        HttpResponse response = execute(client, request);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return Optional.ofNullable(read(response, ref));
        }
        return Optional.empty();
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T read(HttpResponse response, TypeReference<T> ref) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return (T) mapper.readValue(response.getEntity().getContent(), ref);
        }
        catch (UnsupportedOperationException | IOException e) {
            throw new RunnerException("Error occured reading http response", e);
        }
    }
}
