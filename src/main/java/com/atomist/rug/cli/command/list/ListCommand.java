package com.atomist.rug.cli.command.list;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.ArtifactDescriptor.Scope;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;
import com.atomist.source.file.ZipFileArtifactSourceReader;
import com.atomist.source.file.ZipFileInput;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;

public class ListCommand extends AbstractAnnotationBasedCommand {

    private Log log = new Log(ListCommand.class);

    private void mergeContent(Map<String, Object> scopes) {
        scopes.put("divider", Constants.DIVIDER);

        Handlebars handlebars = new Handlebars();
        handlebars.registerHelpers(new HandlebarsHelpers());
        Template template;
        try {
            template = handlebars.compile("templates/list");
            log.info(template.apply(scopes));
        }
        catch (IOException e) {
        }
    }


    @Command
    public void run(@Option("filter") Properties filter) {

        Map<String, List<ArtifactDescriptor>> archives = new ProgressReportingOperationRunner<Map<String, List<ArtifactDescriptor>>>(
                "Listing local archives")
                        .run(indicator -> collectArchives(filter).stream().collect(
                                Collectors.groupingBy(a -> a.group() + ":" + a.artifact())));
        List<Archive> test = archives.entrySet().stream()
                .map(e -> new Archive(e.getKey(), e.getValue())).collect(Collectors.toList());
        test.sort((a1, a2) -> a1.getName().compareTo(a2.getName()));
        Map<String, Object> scopes = new HashMap<>();
        scopes.put("title", "Local Archives");
        scopes.put("archives", test);
        scopes.put("footer",
                String.format("\nFor more information on specific archive version, run:\n"
                        + "  %s describe archive ARCHIVE -a VERSION", Constants.COMMAND));
        mergeContent(scopes);

        // log.info(Style.yellow(" %s", groupArtifact) + " (%s)",
        // StringUtils.collectionToDelimitedString(versionStrings, ", "));
    }

    protected List<ArtifactDescriptor> collectArchives(Properties filter) {
        File repo = new File(new SettingsReader().read().getLocalRepository().path());
        URI repoHome = repo.toURI();
        if (repo.exists()) {
            Collection<File> archives = FileUtils.listFiles(repo, new String[] { "zip" }, true)
                    .stream()
                    .sorted((o1, o2) -> o1.getAbsolutePath().compareTo(o2.getAbsolutePath()))
                    .collect(Collectors.toList());

            String group = filter.getProperty("group");
            if (group != null) {
                String pattern = "/**/" + group.replace(".", File.separator) + "/*/*/*.zip";
                PathMatcher matcher = new AntPathMatcher();
                archives = archives.stream()
                        .filter(a -> matcher.match(pattern, a.getAbsolutePath()))
                        .collect(Collectors.toList());
            }
            String artifact = filter.getProperty("artifact");
            if (artifact != null) {
                String pattern = "/**/" + artifact + "/*/*.zip";
                PathMatcher matcher = new AntPathMatcher();
                archives = archives.stream()
                        .filter(a -> matcher.match(pattern, a.getAbsolutePath()))
                        .collect(Collectors.toList());
            }
            String version = filter.getProperty("version");
            if (version != null) {
                VersionScheme scheme = new GenericVersionScheme();
                try {
                    VersionConstraint constraint = scheme.parseVersionConstraint(version);
                    archives = archives.stream().filter(a -> {
                        String v = a.getParentFile().getName();
                        try {
                            return constraint.containsVersion(scheme.parseVersion(v));
                        }
                        catch (InvalidVersionSpecificationException e) {
                            return false;
                        }
                    }).collect(Collectors.toList());
                }
                catch (InvalidVersionSpecificationException e) {
                    log.info("Invalid version constraint %s specified in filter", version);
                }
            }
            return archives.stream().filter(f -> {
                try {
                    // filter out all non-Rug archives
                    return ZipFileArtifactSourceReader
                            .fromZipSource(new ZipFileInput(new FileInputStream(f)))
                            .findDirectory(".atomist").isDefined();
                }
                catch (FileNotFoundException e) {
                    return false;
                }
            }).map(f -> {
                URI relativeUri = repoHome.relativize(f.toURI());
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
                return new DefaultArtifactDescriptor(g, a, v, Extension.ZIP, Scope.COMPILE,
                        f.toURI());
            }).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private static class Archive {

        private String name;

        private String version;
        private String versions;

        public Archive(String name, List<ArtifactDescriptor> artifacts) {
            List<String> versionStrings = artifacts.stream()
                    .sorted((v1, v2) -> v2.version().compareTo(v1.version()))
                    .map(ArtifactDescriptor::version).collect(Collectors.toList());
            this.version = versionStrings.get(0);
            versionStrings.remove(0);
            this.versions = StringUtils.collectionToDelimitedString(versionStrings, ", ");
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getVersions() {
            if (versions != null && versions.length() > 0) {
                return ", " + versions;
            }
            return versions;
        }

        public String getVersion() {
            return version;
        }
    }

    public class HandlebarsHelpers {
        public CharSequence empty(Object obj1, Options options) throws IOException {
            Collection<?> collection = (Collection<?>) obj1;
            return collection.isEmpty() ? options.fn() : options.inverse();
        }

        public CharSequence cyan(Object obj1) throws IOException {
            return Style.cyan(obj1.toString());
        }

        public CharSequence bold(Object obj1) throws IOException {
            return Style.bold(obj1.toString());
        }

        public CharSequence underline(Object obj1) throws IOException {
            return Style.underline(obj1.toString());
        }

        public CharSequence yellow(Object obj1) throws IOException {
            return Style.yellow(obj1.toString());
        }
    }
}