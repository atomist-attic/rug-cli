package com.atomist.rug.cli.command;

import static scala.collection.JavaConversions.asJavaCollection;

import java.io.Console;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.text.WordUtils;

import com.atomist.param.Parameter;
import com.atomist.param.ParameterValue;
import com.atomist.param.SimpleParameterValue;
import com.atomist.project.ProjectOperation;
import com.atomist.project.ProjectOperationArguments;
import com.atomist.project.SimpleProjectOperationArguments;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;

import scala.collection.JavaConversions;

public abstract class AbstractParameterizedCommand extends AbstractAnnotationBasedCommand {

    private Log log = new Log(getClass());

    protected ProjectOperationArguments validate(ArtifactDescriptor artifact,
            ProjectOperation operation, ProjectOperationArguments arguments) {

        arguments = collectParameters(operation, arguments);
        validateCollectedParameters(artifact, operation, arguments);

        log.newline();

        return arguments;
    }

    private ProjectOperationArguments collectParameters(ProjectOperation operation,
            ProjectOperationArguments arguments) {
        Collection<Parameter> parameters = asJavaCollection(operation.parameters());
        if (CommandLineOptions.hasOption("I") && !parameters.isEmpty()) {

            Console console = System.console();
            if (console != null) {
                List<ParameterValue> newValues = new ArrayList<>();

                log.newline();
                log.info(Style.cyan(Constants.DIVIDER) + " "
                        + Style.bold("Please specify parameter values"));

                for (Parameter parameter : parameters) {
                    log.newline();

                    ParameterValue pv = JavaConversions.mapAsJavaMap(arguments.parameterValueMap())
                            .get(parameter.getName());
                    String defaultValue = (pv != null ? pv.getValue().toString()
                            : parameter.getDefaultValue());

                    log.info("  " + WordUtils.wrap(parameter.getDescription(),
                            Constants.WRAP_LENGTH, "\n  ", false));
                    log.info("    pattern: %s, min length: %s, max length: %s",
                            parameter.getPattern(), parameter.getMinLength(),
                            parameter.getMaxLength());

                    String value = console.readLine("  %s %s %s ", Style.cyan(Constants.DIVIDER),
                            Style.yellow(parameter.getName()),
                            (defaultValue != null && defaultValue.length() > 0
                                    ? "[" + defaultValue + "] :" : ":"));

                    if (value == null || value.length() == 0) {
                        value = defaultValue;
                    }

                    if (value != null && value.length() > 0) {
                        newValues.add(new SimpleParameterValue(parameter.getName(), value));
                    }
                }

                arguments = new SimpleProjectOperationArguments(arguments.name(),
                        JavaConversions.asScalaBuffer(newValues));
            }
            else {
                log.error("Can't enable interactive mode due to missing console");
            }
        }
        return arguments;
    }

    private void validateCollectedParameters(ArtifactDescriptor artifact,
            ProjectOperation operation, ProjectOperationArguments arguments) {
        Collection<ParameterValue> invalid = asJavaCollection(
                operation.findInvalidParameterValues(arguments));
        Collection<Parameter> missing = asJavaCollection(
                operation.findMissingParameters(arguments));

        if (!invalid.isEmpty() || !missing.isEmpty()) {
            log.newline();
            if (!missing.isEmpty()) {
                log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Missing parameter %s",
                        StringUtils.puralize("value", missing)));
                missing.forEach(p -> log.info("  " + Style.yellow(p.getName())));
            }
            if (!invalid.isEmpty()) {
                log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Invalid parameter %s",
                        StringUtils.puralize("value", invalid)));
                invalid.forEach(
                        p -> log.info("  " + Style.yellow(p.getName()) + " = " + p.getValue()));
            }
            throw new CommandException(String.format("Missing and/or invalid parameters for %s",
                    StringUtils.stripName(operation.name(), artifact)));
        }
    }

}
