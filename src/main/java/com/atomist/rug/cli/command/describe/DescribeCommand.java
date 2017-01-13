package com.atomist.rug.cli.command.describe;

import static scala.collection.JavaConversions.asJavaCollection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.text.WordUtils;

import com.atomist.event.SystemEventHandler;
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
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.utils.OperationUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.loader.OperationsAndHandlers;
import com.atomist.rug.manifest.Manifest;
import com.atomist.rug.manifest.ManifestException;
import com.atomist.rug.manifest.ManifestFactory;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;

import scala.collection.Seq;

public class DescribeCommand extends AbstractAnnotationBasedCommand {

    private static final DescribeLabels EDITOR_LABELS = new DescribeLabels("edit", "editor",
            "Editors");
    private static final DescribeLabels GENERATOR_LABELS = new DescribeLabels("generate",
            "generator", "Generators");
    private static final DescribeLabels EXECUTOR_LABELS = new DescribeLabels("execute", "executor",
            "Executors");
    private static final DescribeLabels REVIEWER_LABELS = new DescribeLabels("review", "reviewer",
            "Reviewers");

    private Log log = new Log(getClass());

    @Command
    public void run(OperationsAndHandlers operationsAndHandlers, ArtifactDescriptor artifact,
            ArtifactSource source, @Argument(index = 1, defaultValue = "") String kind,
            @Argument(index = 2) String name) {

        Operations operations = operationsAndHandlers.operations();
        String operationName = OperationUtils.extractRugTypeName(name);

        switch (kind) {
        case "editor":
            describeOperations(artifact, operationName, operations.editors(), EDITOR_LABELS);
            break;
        case "generator":
            describeOperations(artifact, operationName, operations.generators(), GENERATOR_LABELS);
            break;
        case "reviewer":
            describeOperations(artifact, operationName, operations.reviewers(), REVIEWER_LABELS);
            break;
        case "executor":
            describeOperations(artifact, operationName, operations.executors(), EXECUTOR_LABELS);
            break;
        case "archive":
            describeArchive(artifact, source, operationsAndHandlers);
            break;
        case "":
            throw new CommandException("Please tell me the TYPE and the NAME of what you would like me to describe.", "describe");
        default:
            if (kind.split(":").length == 2) {
                throw new CommandException("It looks like you're trying to describe an archive. Please try: rug describe archive " + kind, "describe");
            }
            if (kind.split(":").length == 3) {
                throw new CommandException("Please tell me what kind of thing to describe. Try: rug describe [editor|generator|executor] " + kind, "describe");
            }
            throw new CommandException("Invalid TYPE provided. Please tell me what you would like to describe: archive, editor, generator, or executor.", "describe");
        }
    }

    private void describeArchive(ArtifactDescriptor artifact, ArtifactSource source,
            OperationsAndHandlers operationsAndHandlers) {
        try {
            log.newline();
            Manifest manifest = ManifestFactory.read(source);
            describeName(manifest);
            describeProvenanceInfo(manifest);
            describeContents(artifact, source);
            describeOperations(artifact, operationsAndHandlers);
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
        return WordUtils.wrap(p.getDescription(), Constants.WRAP_LENGTH, "\n    ", false);

    }

    private void describeInvoke(ArtifactDescriptor artifact, ProjectOperationInfo info,
            String command, String type) {
        log.newline();
        log.info("To invoke the %s %s, run:", StringUtils.stripName(info.name(), artifact), type);
        StringBuilder invokeSb = new StringBuilder();
        if (info instanceof ProjectGenerator) {
            invokeSb.append("PROJECT_NAME ");
            asJavaCollection(info.parameters()).stream()
                    .filter(p -> !p.getName().equals("project_name"))
                    .forEach(p -> invokeSb.append(p.getName()).append("=VALUE "));

        }
        else {
            asJavaCollection(info.parameters())
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

    @SuppressWarnings("unchecked")
    private void describeOperations(ArtifactDescriptor artifact, String name, Seq<?> operations,
            DescribeLabels labels) {
        Collection<ProjectOperationInfo> ops = (Collection<ProjectOperationInfo>) asJavaCollection(
                operations);
        String fqName = artifact.group() + "." + artifact.artifact() + "." + name;
        Optional<ProjectOperationInfo> opt = ops.stream().filter(g -> g.name().equals(name))
                .findFirst();
        if (!opt.isPresent()) {
            // try again with a proper namespaced name
            opt = ops.stream().filter(g -> g.name().equals(fqName)).findFirst();
        }

        log.newline();
        if (opt.isPresent()) {
            describeProjectOperationInfo(artifact, opt.get(), labels.operation(), labels.command());
        }
        else {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold(labels.label()));
            ops.forEach(e -> log.info("  " + Style.yellow(StringUtils.stripName(e.name(), artifact))
                    + "\n    "
                    + WordUtils.wrap(e.description(), Constants.WRAP_LENGTH, "\n    ", false)));
            if (name != null) {
                StringUtils.printClosestMatch(StringUtils.stripName(fqName, artifact), artifact,
                        ops.stream().map(o -> o.name()).collect(Collectors.toList()));
                throw new CommandException(String.format(
                        "Specified %s %s could not be found in %s:%s:%s", labels.operation(), name,
                        artifact.group(), artifact.artifact(), artifact.version()));
            }
            else {
                describeInvokeArchive();
            }
        }
    }

    private void describeOperations(ArtifactDescriptor artifact,
            OperationsAndHandlers operationsAndHandlers) {
        Operations operations = operationsAndHandlers.operations();
        Collection<ProjectEditor> editors = asJavaCollection(operations.editors());
        Collection<ProjectGenerator> generators = asJavaCollection(operations.generators());
        Collection<Executor> executors = asJavaCollection(operations.executors());
        Collection<ProjectReviewer> reviewers = asJavaCollection(operations.reviewers());
        Collection<SystemEventHandler> handlers = operationsAndHandlers.handlers().handlers();
        log.newline();
        if (!generators.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Generators"));
            listOperations(artifact, generators);
        }
        if (!editors.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Editors"));
            listOperations(artifact, editors);
        }
        if (!executors.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Executors"));
            listOperations(artifact, executors);
        }
        if (!reviewers.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Reviewers"));
            listOperations(artifact, reviewers);
        }
        if (!handlers.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Handlers"));
            handlers.forEach(
                    e -> log.info("  " + Style.yellow(StringUtils.stripName(e.name(), artifact))));
        }
    }

    private void describeParameters(List<Parameter> required) {
        required.forEach(p -> log.info(
                "  " + Style.yellow(p.getName())
                        + " (%s)\n    %s\n      Pattern: %s, min length: %s, max length: %s",
                p.getDisplayName(), describeDescription(p), describePattern(p),
                (p.getMinLength() >= 0 ? p.getMinLength() : "not defined"),
                (p.getMaxLength() >= 0 ? p.getMaxLength() : "not defined")));
    }

    private void describeParameters(Parameterized parameterized) {
        List<Parameter> parameters = asJavaCollection(parameterized.parameters()).stream()
                .collect(Collectors.toList());

        if (!parameters.isEmpty()) {
            List<Parameter> required = parameters.stream().filter(Parameter::isRequired)
                    .collect(Collectors.toList());
            List<Parameter> optional = parameters.stream().filter(p -> !p.isRequired())
                    .collect(Collectors.toList());

            if (!required.isEmpty()) {
                log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Parameters (required)"));
                describeParameters(required);
            }
            if (!optional.isEmpty()) {
                log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Parameters (optional)"));
                describeParameters(optional);
            }
        }
        else {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Parameters"));
            log.info("  no parameters needed");
        }
    }

    private String describePattern(Parameter p) {
        return p.getPattern();
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

    private void describeTags(ProjectOperationInfo info) {
        if (!info.tags().isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Tags"));
            asJavaCollection(info.tags()).forEach(
                    t -> log.info("  " + Style.yellow(t.name()) + " (" + t.description() + ")"));
        }
    }

    private void listOperations(ArtifactDescriptor artifact, Collection<?> operations) {
        operations.forEach(e -> {
            ProjectOperationInfo info = (ProjectOperationInfo) e;
            log.info("  " + Style.yellow(StringUtils.stripName(info.name(), artifact)) + "\n    "
                    + WordUtils.wrap(info.description(), Constants.WRAP_LENGTH, "\n    ", false));
        });
    }

    private static class DescribeLabels {
        private final String command;
        private final String operation;
        private final String label;

        public DescribeLabels(String command, String operation, String label) {
            this.command = command;
            this.operation = operation;
            this.label = label;
        }

        public String command() {
            return command;
        }

        public String label() {
            return label;
        }

        public String operation() {
            return operation;
        }
    }
}
