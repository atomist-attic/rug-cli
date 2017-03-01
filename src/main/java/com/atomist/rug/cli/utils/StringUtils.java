package com.atomist.rug.cli.utils;

import static scala.collection.JavaConversions.asJavaCollection;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.util.SystemPropertyUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.resolver.ArtifactDescriptor;

import scala.collection.Seq;

public abstract class StringUtils {

    private static final Log log = new Log(StringUtils.class);

    public static void printClosestMatch(String name, ArtifactDescriptor artifact,
            Seq<String> nameOptions) {
        printClosestMatch(name, artifact, asJavaCollection(nameOptions));
    }

    public static void printClosestMatch(String name, ArtifactDescriptor artifact,
            Collection<String> nameOptions) {
        Optional<String> closestMatch = StringUtils.computeClosestMatch(name, nameOptions);
        if (closestMatch.isPresent()) {
            log.newline();
            log.info(Constants.CLOSEST_MATCH_HINT);
            log.info("  %s", StringUtils.stripName(closestMatch.get(), artifact));
        }
    }

    public static Optional<String> computeClosestMatch(String searchTerm,
            Collection<String> searchBase) {
        Map<String, Integer> distances = searchBase.stream().collect(Collectors.toMap(s -> s,
                s -> LevenshteinDistance.computeLevenshteinDistance(searchTerm, s)));
        return distances.entrySet().stream().filter(e -> e.getValue() <= 3)
                .min(Comparator.comparing(Map.Entry::getValue)).map(Map.Entry::getKey);
    }

    public static String puralize(String value, Collection<?> items) {
        return puralize(value, value + "s", items);
    }

    public static String puralize(String singular, String pural, Collection<?> items) {
        if (items.size() > 1) {
            return pural;
        }
        else {
            return singular;
        }
    }

    public static String expandEnvironmentVarsAndHomeDir(String text) {
        if (text == null) {
            return text;
        }
        text = text.replace("~", "${user.home}");
        return expandEnvironmentVars(text);
    }

    public static String expandEnvironmentVars(String text) {
        if (text == null) {
            return text;
        }
        return SystemPropertyUtils.resolvePlaceholders(text);
    }

    public static String stripName(String name, ArtifactDescriptor artifact) {
        return name.replace(artifact.group() + "." + artifact.artifact() + ".", "");
    }

    private static class LevenshteinDistance {
        public static int computeLevenshteinDistance(CharSequence lhs, CharSequence rhs) {
            int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];

            for (int i = 0; i <= lhs.length(); i++)
                distance[i][0] = i;
            for (int j = 1; j <= rhs.length(); j++)
                distance[0][j] = j;

            for (int i = 1; i <= lhs.length(); i++)
                for (int j = 1; j <= rhs.length(); j++)
                    distance[i][j] = minimum(distance[i - 1][j] + 1, distance[i][j - 1] + 1,
                            distance[i - 1][j - 1]
                                    + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1));

            return distance[lhs.length()][rhs.length()];
        }

        private static int minimum(int a, int b, int c) {
            return Math.min(Math.min(a, b), c);
        }
    }

}
