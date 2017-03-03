package com.atomist.rug.cli;

import java.io.IOException;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import com.atomist.rug.cli.utils.HttpClientFactory;
import com.jayway.jsonpath.JsonPath;

public class DownloadStats {

    public static void main(String[] args) throws UnsupportedOperationException, IOException {
        System.out.println("rug-cli (debian): " + countFiles("debian/pool", u -> true));
        System.out.println("rug-cli (windows): " + countFiles("nuget/rug-cli", u -> true));
        System.out.println("rug-cli (yum): " + countFiles("yum", u -> true));
        System.out.println("rug-cli: " + count("rug-cli", u -> u.endsWith("-bin.tar.gz") || u.endsWith("-bin.zip")));
        System.out.println("rug: " + count("rug", u -> !u.endsWith("-sources.jar") && !u.endsWith("-tests.jar") && !u.endsWith(".pom")));
    }

    protected static long count(String group, Function<String, Boolean> filter) throws IOException {
        String url = "https://atomist.jfrog.io/atomist/api/search/gavc?g=com.atomist&a=" + group + "&repos=libs-release-local";
        HttpClient client = HttpClientFactory.httpClient(url);
        HttpGet get = new HttpGet(url);
        HttpClientFactory.authorizationHeader(get, "christian", "BsS-5Yh-s3Y-TCd");
        
        HttpResponse response = HttpClientFactory.execute(client, get);
        List<String> urls = JsonPath.read(response.getEntity().getContent(), "$.results[*].uri");
        IntSummaryStatistics count = urls.stream().filter(u -> filter.apply(u)).map(u -> {
            HttpGet get_ = new HttpGet(u + "?stats");
            HttpResponse response_ = HttpClientFactory.execute(client, get_);
            try {
                int c = JsonPath.read(response_.getEntity().getContent(), "$.downloadCount");
                System.out.println(u + " " + c);
                return c;
            }
            catch (UnsupportedOperationException | IOException e) {
                return 0;
            }
        }).collect(Collectors.summarizingInt(i -> i));
        return count.getSum();
    }

    protected static long countFiles(String folder, Function<String, Boolean> filter) throws IOException {
        String url = "https://atomist.jfrog.io/atomist/api/storage/" + folder;
        HttpClient client = HttpClientFactory.httpClient(url);
        HttpGet get = new HttpGet(url);
        HttpClientFactory.authorizationHeader(get, "christian", "BsS-5Yh-s3Y-TCd");
        
        HttpResponse response = HttpClientFactory.execute(client, get);
        List<String> urls = JsonPath.read(response.getEntity().getContent(), "$.children[*].uri");
        IntSummaryStatistics count = urls.stream().filter(u -> filter.apply(u)).map(u -> {
            HttpGet get_ = new HttpGet(url + u + "?stats");
            HttpResponse response_ = HttpClientFactory.execute(client, get_);
            try {
                int c = JsonPath.read(response_.getEntity().getContent(), "$.downloadCount");
                System.out.println(url + u + " " + c);
                return c;
            }
            catch (UnsupportedOperationException | IOException e) {
                e.printStackTrace();
                return 0;
            }
        }).collect(Collectors.summarizingInt(i -> i));
        return count.getSum();
    }

}
