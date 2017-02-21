package com.atomist.rug.cli.command.describe;

import com.atomist.param.Parameter;
import com.atomist.param.Parameterized;
import com.atomist.project.archive.Rugs;
import com.atomist.project.edit.ProjectEditor;
import com.atomist.project.generate.ProjectGenerator;
import com.atomist.project.review.ProjectReviewer;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.annotation.Validator;
import com.atomist.rug.cli.command.utils.OperationUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.manifest.Manifest;
import com.atomist.rug.resolver.manifest.ManifestFactory;
import com.atomist.rug.resolver.metadata.MetadataWriter;
import com.atomist.rug.resolver.project.GitInfo;
import com.atomist.rug.resolver.project.ProvenanceInfoArtifactSourceReader;
import com.atomist.rug.runtime.CommandHandler;
import com.atomist.rug.runtime.EventHandler;
import com.atomist.rug.runtime.ParameterizedRug;
import com.atomist.rug.runtime.ResponseHandler;
import com.atomist.rug.runtime.Rug;
import com.atomist.source.ArtifactSource;
import com.atomist.source.FileArtifact;
import org.apache.commons.lang3.text.WordUtils;
import scala.collection.Seq;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static scala.collection.JavaConversions.asJavaCollection;

public class DescribeCommand extends AbstractAnnotationBasedCommand {

    private static final DescribeLabels EDITOR_LABELS = new DescribeLabels("edit", "editor",
            "Editors");
    private static final DescribeLabels GENERATOR_LABELS = new DescribeLabels("generate",
            "generator", "Generators");
    private static final DescribeLabels REVIEWER_LABELS = new DescribeLabels("review", "reviewer",
            "Reviewers");
    private static final DescribeLabels COMMAND_HANDLER_LABELS = new DescribeLabels("command",
            "command-handler", "Command Handlers");
    private static final DescribeLabels EVENT_HANDLER_LABELS = new DescribeLabels("trigger",
            "event-handler", "Event Handlers");
    private static final DescribeLabels RESPONSE_HANDLER_LABELS = new DescribeLabels("respond",
            "response-handler", "Response Handlers");

    private static final String INVALID_TYPE_MESSAGE = "Invalid TYPE provided. Please tell me what you would like to describe: archive, editor, generator, reviewer, command-handler, event-handler, or response-handler ";

    @Validator
    public void validate(@Argument(index = 1, defaultValue = "") String kind,
            @Argument(index = 2) String name, @Option("output") String format) {
        switch (kind) {
        case "editor":
            break;
        case "generator":
            break;
        case "reviewer":
            break;
        case "command-handler":
            break;
        case "event-handler":
            break;
        case "response-handler":
            break;
        case "archive":
            validateFormat(format);
            break;
        case "":
            throw new CommandException(INVALID_TYPE_MESSAGE, "describe");
        default:
            if (kind.split(":").length == 2) {
                throw new CommandException(
                        "It looks like you're trying to describe an archive. Please try:\n  rug describe archive "
                                + kind,
                        "describe");
            }
            if (kind.split(":").length == 3) {
                throw new CommandException(
                        "Please tell me what kind of thing to describe. Try:\n  rug describe editor|generator|reviewer|event-handler|command-handler|response-handler "
                                + kind,
                        "describe");
            }
            throw new CommandException(INVALID_TYPE_MESSAGE, "describe");
        }
    }

    @Command
    public void run(Rugs operations, ArtifactDescriptor artifact, ArtifactSource source,
            @Argument(index = 1, defaultValue = "") String kind, @Argument(index = 2) String name,
            @Option("output") String format) {

        String operationName = OperationUtils.extractRugTypeName(name);

        switch (kind) {
        case "editor":
            describeRugs(artifact, operationName, operations.editors(), EDITOR_LABELS);
            break;
        case "generator":
            describeRugs(artifact, operationName, operations.generators(), GENERATOR_LABELS);
            break;
        case "reviewer":
            describeRugs(artifact, operationName, operations.reviewers(), REVIEWER_LABELS);
            break;
        case "command-handler":
            describeRugs(artifact, operationName, operations.commandHandlers(),
                    COMMAND_HANDLER_LABELS);
            break;

        case "event-handler":
            describeRugs(artifact, operationName, operations.eventHandlers(), EVENT_HANDLER_LABELS);
            break;

        case "response-handler":
            describeRugs(artifact, operationName, operations.responseHandlers(),
                    RESPONSE_HANDLER_LABELS);
            break;
        case "archive":
            describeArchive(operations, artifact, source, format);
            break;
        }
    }

    private void describeArchive(ArtifactDescriptor artifact, ArtifactSource source,
            Rugs operationsAndHandlers) {
        log.newline();
        Manifest manifest = ManifestFactory.read(source);
        describeName(manifest);
        describeProvenanceInfo(manifest);
        describeContents(artifact, source);
        describeRugs(artifact, operationsAndHandlers);
        describeDependencies(manifest);
        describeInvokeArchive();
    }

    private void describeArchive(Rugs rugs, ArtifactDescriptor artifact, ArtifactSource source,
            String format) {
        if (format != null) {
            validateFormat(format);

            Optional<GitInfo> info = ProvenanceInfoArtifactSourceReader.read(source);
            FileArtifact metadata = MetadataWriter.create(rugs, artifact, source, info.orElse(null),
                    MetadataWriter.Format.valueOf(format.toUpperCase()));

            System.out.println(metadata.content());
        }
        else {
            describeArchive(artifact, source, rugs);
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
        StringBuilder sb = new StringBuilder();
        if (p.getMinLength() >= 0) {
            sb.append("min: ").append(p.getMinLength()).append("  ");
        }
        if (p.getMaxLength() >= 0) {
            sb.append("max: ").append(p.getMaxLength()).append("  ");
        }
        if (p.getDefaultValue() != null && p.getDefaultValue().length() > 0) {
            sb.append("default: ").append(p.getDefaultValue()).append("  ");
        }
        if (p.getValidInputDescription() != null && p.getValidInputDescription().length() > 0) {
            sb.append("valid input: ").append(
                    org.apache.commons.lang3.StringUtils.capitalize(p.getValidInputDescription()))
                    .append("  ");
        }
        if (p.getPattern() != null && p.getPattern().length() > 0) {
            if (p.getPattern().length() > 40) {
                sb.append("pattern: ")
                        .append(org.apache.commons.lang3.StringUtils.truncate(p.getPattern(), 40))
                        .append("...");
            }
            else {
                sb.append("pattern: ").append(p.getPattern());
            }
        }
        String detail = sb.toString();
        sb = new StringBuilder();
        if (p.description() != null && !p.name().equals(p.description())) {
            sb.append(
                    WordUtils.wrap(org.apache.commons.lang3.StringUtils.capitalize(p.description()),
                            Constants.WRAP_LENGTH, "\n    ", false).trim());
            sb.append("\n    ");
        }
        sb.append(WordUtils.wrap(detail, Constants.WRAP_LENGTH, "\n    ", false).trim());

        String wrapped = sb.toString();
        wrapped = wrapped.replace("min:", Style.gray("min:"));
        wrapped = wrapped.replace("max:", Style.gray("max:"));
        wrapped = wrapped.replace("default:", Style.gray("default:"));
        wrapped = wrapped.replace("valid input:", Style.gray("valid input:"));
        wrapped = wrapped.replace("pattern:", Style.gray("pattern:"));
        return wrapped;

    }

    private String describeDisplayName(Parameter p) {
        return (p.getDisplayName() != null ? "(" + p.getDisplayName() + ")" : "");
    }

    private void describeInvoke(ArtifactDescriptor artifact, Rug info,

            String command, String type) {
        log.newline();
        log.info("To invoke the %s %s, run:", StringUtils.stripName(info.name(), artifact), type);
        StringBuilder invokeSb = new StringBuilder();
        if (info instanceof ProjectGenerator) {
            ProjectGenerator generator = (ProjectGenerator) info;
            invokeSb.append("PROJECT_NAME ");
            asJavaCollection(generator.parameters()).stream()
                    .filter(p -> !p.getName().equals("project_name"))
                    .forEach(p -> invokeSb.append(p.getName()).append("=VALUE "));

        }
        else if (info instanceof ParameterizedRug) {
            ParameterizedRug parameterizedRug = (ParameterizedRug) info;
            asJavaCollection(parameterizedRug.parameters())
                    .forEach(p -> invokeSb.append(p.getName()).append("=VALUE "));
        }
        if (Constants.isShell()) {
            log.info("  %s %s %s", command, StringUtils.stripName(info.name(), artifact),
                    invokeSb.toString());
        }
        else {
            log.info("  %s%s \"%s:%s:%s\" -a %s%s %s", Constants.command(), command,
                    artifact.group(), artifact.artifact(),
                    StringUtils.stripName(info.name(), artifact), artifact.version(),
                    (CommandLineOptions.hasOption("l") ? " -l" : ""), invokeSb.toString());
        }
    }

    private void describeInvokeArchive() {
        log.newline();
        log.info("To get more information on any of the Rugs listed above, run:");
        log.info(
                "  %s%s editor|generator|reviewer|command-handler|event-handler|response-handler ARTIFACT %s",
                Constants.command(), "describe", (CommandLineOptions.hasOption("l") ? "-l" : ""));
    }

    private void describeName(ArtifactDescriptor artifact, Rug info) {
        String name = info.name();
        log.info(Style.bold(Style.yellow(StringUtils.stripName(name, artifact))));
        log.info("%s:%s:%s", artifact.group(), artifact.artifact(), artifact.version());
        log.info(org.apache.commons.lang3.StringUtils.capitalize(info.description()));
    }

    private void describeName(Manifest manifest) {
        log.info(Style.bold(Style.yellow("%s:%s:%s", manifest.group(), manifest.artifact(),
                manifest.version())));
        log.newline();
    }

    private void describeRugs(ArtifactDescriptor artifact, Rugs operations) {
        Collection<ProjectEditor> editors = asJavaCollection(operations.editors()).stream()
                .sorted(Comparator.comparing(Rug::name)).collect(Collectors.toList());
        Collection<ProjectGenerator> generators = asJavaCollection(operations.generators()).stream()
                .sorted(Comparator.comparing(Rug::name)).collect(Collectors.toList());
        Collection<ProjectReviewer> reviewers = asJavaCollection(operations.reviewers()).stream()
                .sorted(Comparator.comparing(Rug::name)).collect(Collectors.toList());
        Collection<CommandHandler> commandHandlers = asJavaCollection(operations.commandHandlers())
                .stream().sorted(Comparator.comparing(Rug::name)).collect(Collectors.toList());
        Collection<EventHandler> eventHandlers = asJavaCollection(operations.eventHandlers())
                .stream().sorted(Comparator.comparing(Rug::name)).collect(Collectors.toList());
        Collection<ResponseHandler> responseHandlers = asJavaCollection(
                operations.responseHandlers()).stream().sorted(Comparator.comparing(Rug::name))
                        .collect(Collectors.toList());
        log.newline();

        if (!generators.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Generators"));
            listOperations(artifact, generators);
        }
        if (!editors.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Editors"));
            listOperations(artifact, editors);
        }

        if (!reviewers.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Reviewers"));
            listOperations(artifact, reviewers);
        }

        if (!commandHandlers.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Command Handlers"));
            listOperations(artifact, commandHandlers);
        }

        if (!eventHandlers.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Event Handlers"));
            listOperations(artifact, eventHandlers);
        }

        if (!responseHandlers.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Response Handlers"));
            listOperations(artifact, responseHandlers);
        }
    }

    @SuppressWarnings("unchecked")
    private void describeRugs(ArtifactDescriptor artifact, String name, Seq<?> operations,
            DescribeLabels labels) {
        Collection<Rug> ops = (Collection<Rug>) asJavaCollection(operations);
        String fqName = artifact.group() + "." + artifact.artifact() + "." + name;
        Optional<Rug> opt = ops.stream().filter(g -> g.name().equals(name)).findFirst();

        if (!opt.isPresent()) {
            // try again with a proper namespaced name
            opt = ops.stream().filter(g -> g.name().equals(fqName)).findFirst();
        }

        if (opt.isPresent()) {
            log.newline();
            describeRug(artifact, opt.get(), labels.operation(), labels.command());
        }
        else {
            if (!ops.isEmpty()) {
                log.newline();
                log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold(labels.label()));
                ops.forEach(e -> log.info("  "
                        + Style.yellow(StringUtils.stripName(e.name(), artifact)) + "\n    "
                        + WordUtils.wrap(e.description(), Constants.WRAP_LENGTH, "\n    ", false)));
            }

            if (name != null) {
                StringUtils.printClosestMatch(StringUtils.stripName(fqName, artifact), artifact,
                        ops.stream().map(Rug::name).collect(Collectors.toList()));
                throw new CommandException(String.format(
                        "Specified %s %s could not be found in %s:%s:%s", labels.operation(), name,
                        artifact.group(), artifact.artifact(), artifact.version()));
            }
            else {
                describeInvokeArchive();
            }
        }
    }

    private void describeEventHandler(EventHandler present) {
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Root node"));
        log.info(Style.yellow("  " + present.rootNodeName()));
    }

    private void describeParameters(List<Parameter> parameters) {
        parameters.forEach(p -> log.info("  " + Style.yellow(p.getName()) + " %s\n    %s",
                describeDisplayName(p), describeDescription(p)));
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

    private void describeRug(ArtifactDescriptor artifact, Rug info, String type, String command) {
        describeName(artifact, info);
        log.newline();
        if (info instanceof CommandHandler) {
            describeIntent((CommandHandler) info);
        }
        if (info instanceof EventHandler) {
            describeEventHandler((EventHandler) info);
        }
        describeTags(info);
        if (info instanceof ParameterizedRug) {
            describeParameters((ParameterizedRug) info);
        }
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

    private void describeIntent(CommandHandler info) {
        if (!info.intent().isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Intent"));
            asJavaCollection(info.intent()).forEach(t -> log.info("  " + Style.yellow(t)));
        }
    }

    private void describeTags(Rug info) {
        if (!info.tags().isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Tags"));
            asJavaCollection(info.tags()).forEach(
                    t -> log.info("  " + Style.yellow(t.name()) + " (" + t.description() + ")"));
        }
    }

    private void listOperations(ArtifactDescriptor artifact, Collection<?> operations) {
        operations.forEach(e -> {
            Rug info = (Rug) e;
            log.info("  " + Style.yellow(StringUtils.stripName(info.name(), artifact)) + "\n    "
                    + WordUtils.wrap(
                            org.apache.commons.lang3.StringUtils.capitalize(info.description()),
                            Constants.WRAP_LENGTH, "\n    ", false));
        });
    }

    private void validateFormat(String format) {
        if (format != null && (!"json".equals(format) && !"yaml".equals(format))) {
            throw new CommandException("Invalid FORMAT provided. Allowed formats are: json or yaml",
                    "describe");
        }
    }

    private static class DescribeLabels {
        private final String command;
        private final String label;
        private final String operation;

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
