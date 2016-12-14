package com.atomist.rug.cli.command.describe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.atomist.param.Parameter;
import com.atomist.param.Parameterized;
import com.atomist.project.Executor;
import com.atomist.project.ProjectOperationInfo;
import com.atomist.project.archive.Operations;
import com.atomist.project.edit.ProjectEditor;
import com.atomist.project.generate.ProjectGenerator;
import com.atomist.project.review.ProjectReviewer;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.manifest.Manifest;
import com.atomist.rug.manifest.ManifestException;
import com.atomist.rug.manifest.ManifestFactory;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;
import com.atomist.source.file.ZipFileArtifactSourceReader;
import com.atomist.source.file.ZipFileInput;

import scala.collection.JavaConverters;

public class DescribeCommand extends AbstractAnnotationBasedCommand {

    private Log log = new Log(getClass());

    @Command
    public void run(Operations operations, ArtifactDescriptor artifact,
            @Argument(index = 1, defaultValue = "") String kind, @Argument(index = 2) String name) {

        switch (kind) {
        case "editor":
            describeEditor(artifact, CommandUtils.extractRugTypeName(name), operations);
            break;
        case "generator":
            describeGenerator(artifact, CommandUtils.extractRugTypeName(name), operations);
            break;
        case "reviewer":
            describeReviewer(artifact, CommandUtils.extractRugTypeName(name), operations);
            break;
        case "executor":
            describeExecutor(artifact, CommandUtils.extractRugTypeName(name), operations);
            break;
        case "archive":
            describeArchive(artifact, operations);
            break;
        default:
            throw new CommandException("No or invalid TYPE provided.", "describe");
        }
    }

    private ArtifactSource createArtifactSource(ArtifactDescriptor artifact) {
        try {
            File archiveRoot = new File(artifact.uri());
            if (archiveRoot.isFile()) {
                return ZipFileArtifactSourceReader
                        .fromZipSource(new ZipFileInput(new FileInputStream(archiveRoot)));
            }
            else {
                return new FileSystemArtifactSource(
                        new SimpleFileSystemArtifactSourceIdentifier(archiveRoot));
            }
        }
        catch (FileNotFoundException e) {
            throw new RunnerException(e);
        }
    }

    private void describeArchive(ArtifactDescriptor artifact, Operations operations) {
        ArtifactSource source = createArtifactSource(artifact);
        try {
            log.newline();
            Manifest manifest = ManifestFactory.read(source);
            describeName(manifest);
            describeProvenanceInfo(manifest);
            describeContents(artifact, source);
            describeOperations(artifact, operations);
            describeDependencies(manifest);
            describeInvokeArchive();
        }
        catch (ManifestException e) {
            throw new RunnerException(e);
        }
    }

    private void describeContents(ArtifactDescriptor artifact, ArtifactSource source) {
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Archive"));
        log.info("  %s (%s in %s files)", Style.underline(FileUtils.relativize(artifact.uri())),
                FileUtils.sizeOf(artifact.uri()), source.allFiles().size());
    }

    private void describeDependencies(Manifest manifest) {
        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Requires"));
        log.info(Style.yellow("  %s", manifest.requires()));
        if (manifest.dependencies().size() > 0) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Dependencies"));
            manifest.dependencies().forEach(d -> log
                    .info(Style.yellow("  %s:%s:%s", d.group(), d.artifact(), d.version())));
        }
        if (manifest.extensions().size() > 0) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Extensions"));
            manifest.extensions().forEach(d -> log
                    .info(Style.yellow("  %s:%s:%s", d.group(), d.artifact(), d.version())));
        }
    }

    private String describeDescription(Parameter p) {
        return p.getDescription() != null && p.getDescription().length() > 0
                ? "(" + p.getDescription() + ")" : "";
    }

    private void describeEditor(ArtifactDescriptor artifact, String name, Operations operations) {
        String fqName = artifact.group() + "." + artifact.artifact() + "." + name;
        Optional<ProjectEditor> opt = JavaConverters.asJavaCollection(operations.editors())
                .stream().filter(g -> g.name().equals(name)).findFirst();
        if (!opt.isPresent()) {
            // try again with a proper namespaced name
            opt = JavaConverters.asJavaCollection(operations.editors()).stream()
                    .filter(g -> g.name().equals(fqName)).findFirst();
        }

        log.newline();
        if (opt.isPresent()) {
            describeProjectOperationInfo(artifact, opt.get(), "editor", "edit");
        }
        else {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Editors"));
            JavaConverters.asJavaCollection(operations.editors()).forEach(
                    e -> log.info("  " + Style.yellow(StringUtils.stripName(e.name(), artifact))
                            + " " + e.description()));
            if (name != null) {
                StringUtils.printClosestMatch(fqName, artifact, operations.editorNames());
                throw new CommandException(
                        String.format("Specified editor %s could not be found in %s:%s:%s", name,
                                artifact.group(), artifact.artifact(), artifact.version()));
            }
            else {
                describeInvokeArchive();
            }
        }
    }

    private void describeExecutor(ArtifactDescriptor artifact, String name, Operations operations) {
        Optional<Executor> opt = JavaConverters.asJavaCollection(operations.executors()).stream()
                .filter(g -> g.name().equals(name)).findFirst();
        String fqName = artifact.group() + "." + artifact.artifact() + "." + name;
        if (!opt.isPresent()) {
            // try again with a proper namespaced name
            opt = JavaConverters.asJavaCollection(operations.executors()).stream()
                    .filter(g -> g.name().equals(fqName)).findFirst();
        }

        log.newline();
        if (opt.isPresent()) {
            describeProjectOperationInfo(artifact, opt.get(), "executor", "execute");

        }
        else {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Executors"));
            JavaConverters.asJavaCollection(operations.executors()).forEach(
                    e -> log.info("  " + Style.yellow(StringUtils.stripName(e.name(), artifact))
                            + " " + e.description()));
            if (name != null) {
                StringUtils.printClosestMatch(fqName, artifact, operations.executorNames());
                throw new CommandException(
                        String.format("Specified executor %s could not be found in %s:%s:%s", name,
                                artifact.group(), artifact.artifact(), artifact.version()));
            }
            else {
                describeInvokeArchive();
            }
        }
    }

    private void describeGenerator(ArtifactDescriptor artifact, String name,
            Operations operations) {
        Optional<ProjectGenerator> opt = JavaConverters.asJavaCollection(operations.generators())
                .stream().filter(g -> g.name().equals(name)).findFirst();

        log.newline();
        if (opt.isPresent()) {
            describeProjectOperationInfo(artifact, opt.get(), "generator", "generate");

        }
        else {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Generators"));
            JavaConverters.asJavaCollection(operations.generators())
                    .forEach(e -> log.info("  " + Style.yellow(e.name()) + " " + e.description()));
            if (name != null) {
                StringUtils.printClosestMatch(name, artifact, operations.generatorNames());
                throw new CommandException(
                        String.format("Specified generator %s could not be found in %s:%s:%s", name,
                                artifact.group(), artifact.artifact(), artifact.version()));
            }
            else {
                describeInvokeArchive();
            }
        }
    }

    private void describeInvoke(ArtifactDescriptor artifact, ProjectOperationInfo info,
            String command, String type) {
        log.newline();
        log.info("To invoke the %s %s, run:", StringUtils.stripName(info.name(), artifact), type);
        StringBuilder invokeSb = new StringBuilder();
        if (info instanceof ProjectGenerator) {
            invokeSb.append("PROJECT_NAME ");
            JavaConverters.asJavaCollection(info.parameters()).stream()
                    .filter(p -> !p.getName().equals("project_name"))
                    .forEach(p -> invokeSb.append(p.getName()).append("=VALUE "));

        }
        else {
            JavaConverters.asJavaCollection(info.parameters())
                    .forEach(p -> invokeSb.append(p.getName()).append("=VALUE "));
        }
        log.info("  %s %s \"%s:%s:%s\" -a %s%s %s", Constants.COMMAND, command, artifact.group(),
                artifact.artifact(), StringUtils.stripName(info.name(), artifact),
                artifact.version(), (CommandLineOptions.hasOption("l") ? " -l" : ""),
                invokeSb.toString());
    }

    private void describeInvokeArchive() {
        log.newline();
        log.info("To get more information on any of the Rugs listed above, run:");
        log.info("  %s %s editor|generator|executor|reviewer ARTIFACT %s", Constants.COMMAND,
                "describe", (CommandLineOptions.hasOption("l") ? "-l" : ""));
    }

    private void describeName(ArtifactDescriptor artifact, ProjectOperationInfo info) {
        String name = info.name();
        log.info(Style.bold(Style.yellow(StringUtils.stripName(name, artifact))));
        log.info("%s:%s:%s", artifact.group(), artifact.artifact(), artifact.version());
        log.info(info.description());
    }

    private void describeName(Manifest manifest) {
        log.info(Style.bold(Style.yellow("%s:%s:%s", manifest.group(), manifest.artifact(),
                manifest.version())));
        log.newline();
    }

    private void describeOperations(ArtifactDescriptor artifact, Operations operations) {
        Collection<ProjectEditor> editors = JavaConverters.asJavaCollection(operations.editors());
        Collection<ProjectGenerator> generators = JavaConverters
                .asJavaCollection(operations.generators());
        Collection<Executor> executors = JavaConverters.asJavaCollection(operations.executors());
        Collection<ProjectReviewer> reviewers = JavaConverters
                .asJavaCollection(operations.reviewers());
        log.newline();
        if (!generators.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Generators"));
            generators.forEach(
                    e -> log.info("  " + Style.yellow(StringUtils.stripName(e.name(), artifact))
                            + " (" + e.description() + ")"));
        }
        if (!editors.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Editors"));
            editors.forEach(e -> log
                    .info("  " + Style.yellow("%s", StringUtils.stripName(e.name(), artifact))
                            + " (" + e.description() + ")"));
        }
        if (!executors.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Executors"));
            executors.forEach(
                    e -> log.info("  " + Style.yellow(StringUtils.stripName(e.name(), artifact))
                            + " (" + e.description() + ")"));
        }
        if (!reviewers.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Reviewers"));
            reviewers.forEach(
                    e -> log.info("  " + Style.yellow(StringUtils.stripName(e.name(), artifact))
                            + " (" + e.description() + ")"));
        }
    }

    private void describeParameters(Parameterized parameterized) {
        List<Parameter> parameters = JavaConverters.asJavaCollection(parameterized.parameters())
                .stream().collect(Collectors.toList());

        if (!parameters.isEmpty()) {
            List<Parameter> required = parameters.stream().filter(Parameter::isRequired)
                    .collect(Collectors.toList());
            List<Parameter> optional = parameters.stream().filter(p -> !p.isRequired())
                    .collect(Collectors.toList());

            if (!required.isEmpty()) {
                log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Parameters (required)"));
                required.forEach(p -> log.info(
                        "  " + Style.yellow(p.getName())
                                + " %s\n    pattern: %s, min length: %s, max length: %s",
                        describeDescription(p), p.getPattern(), p.getMinLength(),
                        p.getMaxLength()));
            }
            if (!optional.isEmpty()) {
                log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Parameters (optional)"));
                optional.forEach(p -> log.info(
                        "  " + Style.yellow(p.getName())
                                + " %s\n    pattern: %s, min length: %s, max length: %s",
                        describeDescription(p), p.getPattern(), p.getMinLength(),
                        p.getMaxLength()));
            }
        }
        else {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Parameters"));
            log.info("  no parameters needed");
        }
    }

    private void describeProjectOperationInfo(ArtifactDescriptor artifact,
            ProjectOperationInfo info, String type, String command) {
        describeName(artifact, info);
        log.newline();
        describeTags(info);
        describeParameters(info);
        describeInvoke(artifact, info, command, type);
    }

    private void describeProvenanceInfo(Manifest manifest) {
        describeProvenanceInfo(manifest.repo(), manifest.branch(), manifest.sha());
    }

    private void describeProvenanceInfo(String repo, String branch, String sha) {
        if (repo != null && branch != null && sha != null) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Origin"));
            log.info("  %s#%s (%s)", repo, branch, sha);
        }
    }

    private void describeReviewer(ArtifactDescriptor artifact, String name, Operations operations) {
        Optional<ProjectReviewer> opt = JavaConverters.asJavaCollection(operations.reviewers())
                .stream().filter(g -> g.name().equals(name)).findFirst();
        String fqName = artifact.group() + "." + artifact.artifact() + "." + name;
        if (!opt.isPresent()) {
            // try again with a proper namespaced name
            opt = JavaConverters.asJavaCollection(operations.reviewers()).stream()
                    .filter(g -> g.name().equals(fqName)).findFirst();
        }

        log.newline();
        if (opt.isPresent()) {
            describeProjectOperationInfo(artifact, opt.get(), "reviewer", "review");

        }
        else {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Reviewers"));
            JavaConverters.asJavaCollection(operations.reviewers()).forEach(
                    e -> log.info("  " + Style.yellow(StringUtils.stripName(e.name(), artifact))
                            + " " + e.description()));
            if (name != null) {
                StringUtils.printClosestMatch(fqName, artifact, operations.reviewerNames());
                throw new CommandException(
                        String.format("Specified reviewer %s could not be found in %s:%s:%s", name,
                                artifact.group(), artifact.artifact(), artifact.version()));
            }
            else {
                describeInvokeArchive();
            }
        }
    }

    private void describeTags(ProjectOperationInfo info) {
        if (!info.tags().isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Tags"));
            JavaConverters.asJavaCollection(info.tags()).forEach(
                    t -> log.info("  " + Style.yellow(t.name()) + " (" + t.description() + ")"));
        }
    }
}
