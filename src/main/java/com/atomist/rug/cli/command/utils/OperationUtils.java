package com.atomist.rug.cli.command.utils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.StringUtils;

import com.atomist.project.archive.Coordinate;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.settings.SettingsReader;

public class OperationUtils {

    public static String extractRugTypeName(String name) {
        if (name != null) {
            String[] parts = name.split(":");
            if (parts.length > 1) {
                return parts[parts.length - 1];
            }
        }
        return name;
    }
    
    public static Coordinate extractFromUrl(URL url) {
        File repo = new File(SettingsReader.read().getLocalRepository().path());
        URI repoHome = repo.toURI();
        try {
            URI relativeUri = repoHome.relativize(url.toURI());
            List<String> segments = new ArrayList<>(
                    Arrays.asList(relativeUri.toString().split("/")));
            // last segment is the actual file name
            segments.remove(segments.size() - 1);
            // last segments is version
            String v = segments.remove(segments.size() - 1);
            // second to last is artifact
            String a = segments.remove(segments.size() - 1);
            // remaining segments are group
            String g = StringUtils.collectionToDelimitedString(segments, ".");
            return new Coordinate(g, a, v);
        }
        catch (URISyntaxException e) {
            throw new RunnerException(e);
        }
    }

}
