package com.atomist.rug.cli.command.describe;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.atomist.param.Parameter;
import com.atomist.param.Parameterized;
import com.atomist.project.archive.Coordinate;
import com.atomist.project.archive.ResolvedDependency;
import com.atomist.project.archive.Rugs;
import com.atomist.project.edit.ProjectEditor;
import com.atomist.project.generate.ProjectGenerator;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.annotation.Validator;
import com.atomist.rug.cli.command.utils.OperationUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.manifest.Manifest;
import com.atomist.rug.resolver.manifest.ManifestFactory;
import com.atomist.rug.resolver.manifest.ManifestUtils;
import com.atomist.rug.resolver.metadata.MetadataWriter;
import com.atomist.rug.resolver.project.GitInfo;
import com.atomist.rug.resolver.project.ProvenanceInfoArtifactSourceReader;
import com.atomist.rug.runtime.CommandHandler;
import com.atomist.rug.runtime.EventHandler;
import com.atomist.rug.runtime.ParameterizedRug;
import com.atomist.rug.runtime.ResponseHandler;
import com.atomist.rug.runtime.Rug;
import com.atomist.rug.runtime.RugScopes;
import com.atomist.rug.runtime.js.JavaScriptCommandHandler;
import com.atomist.rug.runtime.plans.DefaultRugFunctionRegistry;
import com.atomist.rug.spi.MappedParameterizedRug;
import com.atomist.rug.spi.RugFunction;
import com.atomist.rug.spi.SecretAwareRug;
import com.atomist.source.ArtifactSource;
import com.atomist.source.FileArtifact;
import org.apache.commons.lang3.text.WordUtils;
import scala.collection.JavaConverters;
import scala.collection.Seq;

import static scala.collection.JavaConversions.asJavaCollection;

public class DescribeCommand extends AbstractAnnotationBasedCommand {

    private static final DescribeLabels EDITOR_LABELS = new DescribeLabels("edit", "editor",
            "Editors");
    private static final DescribeLabels GENERATOR_LABELS = new DescribeLabels("generate",
            "generator", "Generators");
    private static final DescribeLabels COMMAND_HANDLER_LABELS = new DescribeLabels("command",
            "command-handler", "Command Handlers");
    private static final DescribeLabels EVENT_HANDLER_LABELS = new DescribeLabels("trigger",
            "event-handler", "Event Handlers");
    private static final DescribeLabels RESPONSE_HANDLER_LABELS = new DescribeLabels("respond",
            "response-handler", "Response Handlers");
    private static final DescribeLabels INTEGRATION_TEST_LABELS = new DescribeLabels(
            "integration-test", "integration-test", "Integration Tests");
    private static final DescribeLabels FUNCTION_LABELS = new DescribeLabels(null, "function",
            "Functions");

    private static final Map<Class<?>, DescribeLabels> labelMapping;
    private static final String INVALID_TYPE_MESSAGE = "Invalid TYPE provided. Please use either archive, editor, generator, command-handler, event-handler, response-handler, integration-test, function or dependencies.";

    static {
        labelMapping = new HashMap<>();
        labelMapping.put(ProjectEditor.class, EDITOR_LABELS);
        labelMapping.put(ProjectGenerator.class, GENERATOR_LABELS);
        labelMapping.put(CommandHandler.class, COMMAND_HANDLER_LABELS);
        labelMapping.put(EventHandler.class, EVENT_HANDLER_LABELS);
        labelMapping.put(ResponseHandler.class, RESPONSE_HANDLER_LABELS);
        labelMapping.put(RugFunction.class, FUNCTION_LABELS);
    }

    @Validator
    public void validate(@Argument(index = 1, defaultValue = "") String kind,
            @Argument(index = 2) String name, @Option("output") String format) {
        switch (kind) {
        case "editor":
        case "generator":
        case "command-handler":
        case "event-handler":
        case "response-handler":
        case "integration-test":
        case "function":
        case "dependencies":
            break;
        case "archive":
            validateFormat(format);
            break;
        }
    }

    @Command
    public void run(ResolvedDependency resolvedRugs, ArtifactDescriptor artifact,
            ArtifactSource source, @Argument(index = 1, defaultValue = "") String kind,
            @Argument(index = 2) String name, @Option("output") String format,
            @Option(value = "operations") boolean operations, Settings settings) {

        String operationName = OperationUtils.extractRugTypeName(name);

        switch (kind) {
        case "editor":
            describeRugs(artifact, operationName, resolvedRugs.rugs().editors(), source,
                    EDITOR_LABELS);
            break;
        case "generator":
            describeRugs(artifact, operationName, resolvedRugs.rugs().generators(), source,
                    GENERATOR_LABELS);
            break;
        case "command-handler":
            describeRugs(artifact, operationName, resolvedRugs.rugs().commandHandlers(), source,
                    COMMAND_HANDLER_LABELS);
            break;
        case "event-handler":
            describeRugs(artifact, operationName, resolvedRugs.rugs().eventHandlers(), source,
                    EVENT_HANDLER_LABELS);
            break;
        case "response-handler":
            describeRugs(artifact, operationName, resolvedRugs.rugs().responseHandlers(), source,
                    RESPONSE_HANDLER_LABELS);
            break;
        case "function":
            describeRugs(artifact, operationName,
                    JavaConverters.asScalaBufferConverter(JavaConverters
                            .mapAsJavaMapConverter(DefaultRugFunctionRegistry.providerMap())
                            .asJava().values().stream().collect(Collectors.toList())).asScala(),
                    source, FUNCTION_LABELS);
            break;

        case "integration-test":
            describeRugs(artifact, operationName, resolvedRugs.rugs().commandHandlers(), source,
                    INTEGRATION_TEST_LABELS);
        case "dependencies":
            new DependenciesOperations().run(artifact, resolvedRugs, settings, operations);
            if (operations) {
                describeInvokeArchive(artifact);
            }
            else {
                log.newline();
            }
            break;
        case "archive":
            describeArchive(resolvedRugs.rugs(), artifact, source, format);
            break;
        case "":
            throw new CommandException(INVALID_TYPE_MESSAGE, "describe");
        default:
            // If there is a unique match we don't need the kind
            String rugName = OperationUtils.extractRugTypeName(kind);
            List<Rug> rugs = JavaConverters.asJavaCollectionConverter(resolvedRugs.rugs().allRugs())
                    .asJavaCollection().stream().filter(o -> o.name().equals(rugName))
                    .collect(Collectors.toList());
            if (rugs.size() == 1) {
                Rug rug = rugs.get(0);
                DescribeLabels label = labelMapping.entrySet().stream()
                        .filter(lm -> lm.getKey().isAssignableFrom(rug.getClass())).findFirst()
                        .get().getValue();
                log.newline();
                describeRug(artifact, rug, source, label.label(), label.command());
            }
            else if (kind.split(":").length == 2) {
                throw new CommandException(
                        "It looks like you're trying to describe an archive. Please try:\n  rug describe archive "
                                + kind,
                        "describe");
            }
            else if (kind.split(":").length == 3) {
                throw new CommandException(
                        "Please tell me what kind of thing to describe. Try:\n  rug describe editor|generator|event-handler|command-handler|response-handler|function "
                                + kind,
                        "describe");
            }
            else {
                throw new CommandException(INVALID_TYPE_MESSAGE, "describe");
            }
        }
    }

    private void describeArchive(ArtifactDescriptor artifact, ArtifactSource source,
            Rugs operationsAndHandlers) {
        log.newline();
        Manifest manifest = ManifestFactory.read(source);
        describeName(manifest);
        describeProvenanceInfo(manifest);
        describeContents(artifact, source);
        describeRugs(artifact, operationsAndHandlers, manifest);
        describeDependencies(manifest);
        describeInvokeArchive(artifact);
    }

    private void describeArchive(Rugs rugs, ArtifactDescriptor artifact, ArtifactSource source,
            String format) {
        if (format != null) {
            validateFormat(format);

            Optional<GitInfo> info = ProvenanceInfoArtifactSourceReader.read(source);
            FileArtifact metadata = MetadataWriter.create(rugs, artifact, source, info.orElse(null),
                    Constants.cliClient(), MetadataWriter.Format.valueOf(format.toUpperCase()));

            System.out.println(metadata.content());
        }
        else {
            describeArchive(artifact, source, rugs);
        }
    }

    private void describeContents(ArtifactDescriptor artifact, ArtifactSource source) {
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Archive"));
        log.info("  %s (%s in %s files)",
                Style.underline(FileUtils.relativize(new File(artifact.uri()).toURI())),
                FileUtils.sizeOf(new File(artifact.uri()).toURI()), source.allFiles().size());
    }

    private void describeDependencies(Manifest manifest) {
        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Requires"));
        log.info(Style.yellow("  %s", manifest.requires()));
        if (manifest.dependencies().size() > 0) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Dependencies"));
            manifest.dependencies().forEach(d -> log.info(Style.yellow("  %s:%s %s", d.group(),
                    d.artifact(), formatVersion(d.version()))));
        }
        if (manifest.extensions().size() > 0) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Extensions"));
            manifest.extensions().forEach(d -> log.info(Style.yellow("  %s:%s %s", d.group(),
                    d.artifact(), formatVersion(d.version()))));
        }
    }

    private String formatVersion(String version) {
        if (version.startsWith("(") || version.startsWith("[")) {
            return version;
        }
        else {
            return "(" + version + ")";
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
        if (p.description() != null && !"".equals(p.description())
                && !p.name().equals(p.description())) {
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

    private void describeInvoke(ArtifactDescriptor artifact, Rug info, String command,
            String type) {
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

        if (info instanceof MappedParameterizedRug) {
            asJavaCollection(((MappedParameterizedRug) info).mappedParameters())
                    .forEach(p -> invokeSb.append(p.localKey()).append("=VALUE "));
        }

        if (Constants.isShell()) {
            log.info("  %s %s %s", command, StringUtils.stripName(info.name(), artifact),
                    invokeSb.toString());
        }
        else {
            log.info("  %s%s \"%s:%s:%s\" -a %s%s %s", Constants.command(), command,
                    artifact.group(), artifact.artifact(),
                    StringUtils.stripName(info.name(), artifact), artifact.version(),
                    (CommandLineOptions.hasOption("local") ? " -l" : ""), invokeSb.toString());
        }
    }

    private void describeInvokeArchive(ArtifactDescriptor artifact) {
        log.newline();
        log.info("To get more information on any of the Rugs listed above, run:");
        log.info(
                "  %s%s editor|generator|command-handler|event-handler|response-handler|integration-test|function %s",
                Constants.command(), "describe", (CommandLineOptions.hasOption("local") ? "NAME -l"
                        : artifact.group() + ":" + artifact.artifact() + ":NAME"));
    }

    private void describeName(ArtifactDescriptor artifact, Rug info, ArtifactSource source) {
        String name = info.name();
        Manifest manifest = ManifestFactory.read(source);
        boolean excluded = ManifestUtils.excluded(info, manifest);
        log.info(Style.bold(Style.yellow(StringUtils.stripName(name, artifact)))
                + (excluded ? Style.gray(" (excluded)") : ""));

        if (info instanceof RugFunction) {
            Coordinate c = OperationUtils.extractFromUrl(
                    info.getClass().getProtectionDomain().getCodeSource().getLocation());
            log.info("%s:%s (%s)", c.group(), c.artifact(), c.version());
        }
        else {
            log.info("%s", ArtifactDescriptorUtils.coordinates(artifact));
        }

        log.info(org.apache.commons.lang3.StringUtils.capitalize(info.description()));
    }

    private void describeName(Manifest manifest) {
        log.info(Style.bold(Style.yellow("%s:%s", manifest.group(), manifest.artifact()) + " "
                + Style.gray("(%s)", manifest.version())));
        log.newline();
    }

    private void describeRugs(ArtifactDescriptor artifact, Rugs operations, Manifest manifest) {
        Collection<ProjectEditor> editors = asJavaCollection(operations.editors()).stream()
                .sorted(Comparator.comparing(Rug::name)).collect(Collectors.toList());
        Collection<ProjectGenerator> generators = asJavaCollection(operations.generators()).stream()
                .sorted(Comparator.comparing(Rug::name)).collect(Collectors.toList());
        Collection<CommandHandler> commandHandlers = asJavaCollection(operations.commandHandlers())
                .stream().filter(c -> {
                    if (c instanceof JavaScriptCommandHandler) {
                        JavaScriptCommandHandler jsc = (JavaScriptCommandHandler) c;
                        return jsc.testDescriptor().isEmpty();
                    }
                    else {
                        return false;
                    }
                }).sorted(Comparator.comparing(Rug::name)).collect(Collectors.toList());

        Collection<CommandHandler> testHandlers = asJavaCollection(operations.commandHandlers())
                .stream().filter(c -> {
                    if (c instanceof JavaScriptCommandHandler) {
                        JavaScriptCommandHandler jsc = (JavaScriptCommandHandler) c;
                        return jsc.testDescriptor().nonEmpty()
                                && jsc.testDescriptor().get().kind().equals("integration");
                    }
                    else {
                        return false;
                    }
                }).sorted(Comparator.comparing(Rug::name)).collect(Collectors.toList());

        Collection<EventHandler> eventHandlers = asJavaCollection(operations.eventHandlers())
                .stream().sorted(Comparator.comparing(Rug::name)).collect(Collectors.toList());
        Collection<ResponseHandler> responseHandlers = asJavaCollection(
                operations.responseHandlers()).stream()
                        .filter(r -> r.scope() == RugScopes.DEFAULT$.MODULE$)
                        .sorted(Comparator.comparing(Rug::name)).collect(Collectors.toList());
        log.newline();

        if (!generators.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Generators"));
            listOperations(artifact, generators, manifest);
        }
        if (!editors.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Editors"));
            listOperations(artifact, editors, manifest);
        }

        if (!commandHandlers.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Command Handlers"));
            listOperations(artifact, commandHandlers, manifest);
        }

        if (!eventHandlers.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Event Handlers"));
            listOperations(artifact, eventHandlers, manifest);
        }

        if (!responseHandlers.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Response Handlers"));
            listOperations(artifact, responseHandlers, manifest);
        }

        if (!testHandlers.isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Integration Tests"));
            listOperations(artifact, testHandlers, manifest);
        }
    }

    @SuppressWarnings("unchecked")
    private void describeRugs(ArtifactDescriptor artifact, String name, Seq<?> operations,
            ArtifactSource source, DescribeLabels labels) {
        Collection<Rug> ops = (Collection<Rug>) asJavaCollection(operations);
        String fqName = artifact.group() + "." + artifact.artifact() + "." + name;
        Optional<Rug> opt = ops.stream().filter(g -> g.name().equals(name)).findFirst();

        if (!opt.isPresent()) {
            // try again with a proper namespaced name
            opt = ops.stream().filter(g -> g.name().equals(fqName)).findFirst();
        }

        log.newline();
        if (opt.isPresent()) {
            describeRug(artifact, opt.get(), source, labels.operation(), labels.command());
        }
        else {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold(labels.label()));
            if (!ops.isEmpty()) {
                ops.forEach(e -> log.info("  "
                        + Style.yellow(StringUtils.stripName(e.name(), artifact)) + "\n    "
                        + WordUtils.wrap(
                                org.apache.commons.lang3.StringUtils.capitalize(e.description()),
                                Constants.WRAP_LENGTH, "\n    ", false)));
            }
            else {
                log.info(Style.yellow("  No %s found", labels.label.toLowerCase()));
            }

            if (name != null) {
                StringUtils.printClosestMatch(StringUtils.stripName(fqName, artifact), artifact,
                        ops.stream().map(Rug::name).collect(Collectors.toList()));
                throw new CommandException(
                        String.format("Specified %s %s could not be found in %s:%s",
                                labels.operation(), name, artifact.group(), artifact.artifact()));
            }
            else {
                describeInvokeArchive(artifact);
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

    private void describeRug(ArtifactDescriptor artifact, Rug info, ArtifactSource source,
            String type, String command) {
        describeName(artifact, info, source);
        log.newline();
        if (info instanceof CommandHandler) {
            describeIntent((CommandHandler) info);
            describeSecrets((SecretAwareRug) info);
            describeMappedParameters((MappedParameterizedRug) info);
        }
        if (info instanceof EventHandler) {
            describeEventHandler((EventHandler) info);
            describeSecrets((SecretAwareRug) info);
        }
        if (info instanceof RugFunction) {
            describeSecrets((SecretAwareRug) info);
        }
        describeTags(info);
        if (info instanceof ParameterizedRug && !(info instanceof EventHandler)) {
            describeParameters((ParameterizedRug) info);
        }
        if (command != null) {
            describeInvoke(artifact, info, command, type);
        }
        else {
            log.newline();
        }

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

    private void describeSecrets(SecretAwareRug rug) {
        if (!rug.secrets().isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Secrets"));
            asJavaCollection(rug.secrets()).forEach(t -> log.info("  " + Style.yellow(t.name())));
        }
    }

    private void describeMappedParameters(MappedParameterizedRug rug) {
        if (!rug.mappedParameters().isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Mapped Parameters"));
            asJavaCollection(rug.mappedParameters()).forEach(t -> log.info("  "
                    + Style.yellow(t.localKey()) + " " + Constants.DIVIDER + " " + t.foreignKey()));
        }
    }

    private void describeTags(Rug info) {
        if (!info.tags().isEmpty()) {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Tags"));
            asJavaCollection(info.tags()).forEach(
                    t -> log.info("  " + Style.yellow(t.name()) + " (" + t.description() + ")"));
        }
    }

    private void listOperations(ArtifactDescriptor artifact, Collection<?> operations,
            Manifest manifest) {
        operations.forEach(e -> {
            Rug info = (Rug) e;
            boolean excluded = ManifestUtils.excluded(info, manifest);
            log.info("  " + Style.yellow(StringUtils.stripName(info.name(), artifact))
                    + (excluded ? Style.gray(" (excluded)") : "") + "\n    "
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
