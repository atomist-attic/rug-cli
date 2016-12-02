package com.atomist.rug.cli.command.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import com.atomist.rug.cli.command.annotation.Argument;
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

public class SearchCommand extends AbstractAnnotationBasedCommand {

    private Log log = new Log(SearchCommand.class);

    @Command
    public void run(@Argument(index = 1) String search, @Option("tag") Properties tags) {

        Map<String, List<ArtifactDescriptor>> archives = new ProgressReportingOperationRunner<Map<String, List<ArtifactDescriptor>>>(
                "Searching online catalog")
                        .run(indicator -> {return null;});

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Found Archives"));

        archives.entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                .forEach(a -> printArchive(a.getKey(), a.getValue()));

        log.info("\nFor more information on specific archive version, run:\n"
                + "  %s describe archive ARCHIVE -a VERSION", Constants.COMMAND);
    }

    private void printArchive(String groupArtifact, List<ArtifactDescriptor> versions) {
        List<String> versionStrings = versions.stream()
                .sorted((v1, v2) -> v2.version().compareTo(v1.version()))
                .map(ArtifactDescriptor::version).collect(Collectors.toList());
        // underline the highest version
        versionStrings.set(0, Style.underline(versionStrings.get(0)));
        log.info(Style.yellow("  %s", groupArtifact) + " (%s)",
                StringUtils.collectionToDelimitedString(versionStrings, ", "));
    }

    protected List<ArtifactDescriptor> collectArchives(Properties filter) {
        File repo = new File(new SettingsReader().read().getLocalRepository().path());
        return Collections.emptyList();
    }
}