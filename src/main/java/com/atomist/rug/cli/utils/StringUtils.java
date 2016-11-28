package com.atomist.rug.cli.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.atomist.rug.resolver.ArtifactDescriptor;

public abstract class StringUtils {

    private static final String ENV_VARIABLE_PATTERN_STRING = "(\\$[{]?([A-Z_a-z]+)[}]?)";
    private static final Pattern ENV_VARIABLE_PATTERN = Pattern
            .compile(ENV_VARIABLE_PATTERN_STRING);

    public static Optional<String> computeClosestMatch(String searchTerm,
            Collection<String> searchBase) {
        Map<String, Integer> distances = searchBase.stream().collect(Collectors.toMap(s -> s,
                s -> LevenshteinDistance.computeLevenshteinDistance(searchTerm, s)));
        return distances.entrySet().stream().filter(e -> e.getValue() <= 2)
                .min((e1, e2) -> e1.getValue().compareTo(e2.getValue())).map(Map.Entry::getKey);
    }
    
    public static String puralize(String value, Collection<?> items) {
        if (items.size() > 1) {
            return value + "s";
        }
        else {
            return value;
        }
    }

    public static String expandEnvironmentVars(String text) {
        if (text != null) {
            Matcher matcher = ENV_VARIABLE_PATTERN.matcher(text);

            while (matcher.find()) {
                Properties props = System.getProperties();
                props.putAll(System.getenv());

                String replaceKey = matcher.group(1);
                String key = matcher.group(2);
                text = text.replace(replaceKey, props.getProperty(key, replaceKey));
            }
        }
        return text;
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
