package com.atomist.rug.cli.command;

import static scala.collection.JavaConversions.asJavaCollection;
import static scala.collection.JavaConversions.asScalaBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.text.WordUtils;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;

import com.atomist.param.Parameter;
import com.atomist.param.ParameterValue;
import com.atomist.param.ParameterValues;
import com.atomist.param.SimpleParameterValue;
import com.atomist.param.SimpleParameterValues;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.shell.ShellUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.runtime.ParameterizedRug;

import scala.collection.JavaConversions;

public abstract class AbstractParameterizedCommand extends AbstractAnnotationBasedCommand {

    private ParameterValues collectParameters(ParameterizedRug rug, ParameterValues arguments) {
        Collection<Parameter> parameters = asJavaCollection(rug.parameters());
        if (CommandLineOptions.hasOption("interactive") && !parameters.isEmpty()) {

            LineReader reader = ShellUtils.lineReader(ShellUtils.INTERACTIVE_HISTORY,
                    Optional.empty());

            List<ParameterValue> newValues = new ArrayList<>();

            log.newline();
            log.info(Style.cyan(Constants.DIVIDER) + " "
                    + Style.bold("Please specify parameter values"));
            log.info(Constants.LEFT_PADDING
                    + "Press 'Enter' to accept default or provided values. '*' indicates required parameters.");

            for (Parameter parameter : parameters) {
                log.newline();

                ParameterValue pv = JavaConversions.mapAsJavaMap(arguments.parameterValueMap())
                        .get(parameter.getName());
                String defaultValue = (pv != null ? pv.getValue().toString()
                        : parameter.getDefaultValue());

                String description = org.apache.commons.lang3.StringUtils
                        .capitalize(parameter.getDescription());
                log.info("  " + WordUtils.wrap(description, Constants.WRAP_LENGTH, "\n  ", false));

                pv = readParameter(reader, parameter, defaultValue);

                boolean firstAttempt = true;
                while (isInvalid(rug, pv)
                        || ((pv.getValue() == null || pv.getValue().toString().length() == 0)
                                && parameter.isRequired())) {
                    log.info(Style.red("  Provided value '%s' is not valid", pv.getValue()));
                    if (firstAttempt) {
                        log.newline();
                        log.info("  pattern: %s, min length: %s, max length: %s",
                                parameter.getPattern(),
                                (parameter.getMinLength() >= 0 ? parameter.getMinLength()
                                        : "not defined"),
                                (parameter.getMaxLength() >= 0 ? parameter.getMaxLength()
                                        : "not defined"));
                        firstAttempt = false;
                    }

                    pv = readParameter(reader, parameter, defaultValue);
                }

                // add the new and validated parameter to project operations arguments
                newValues.add(pv);
            }
            arguments = new SimpleParameterValues(JavaConversions.asScalaBuffer(newValues));
            log.newline();

            ShellUtils.shutdown(reader);
        }
        return arguments;
    }

    private boolean isInvalid(ParameterizedRug rug, ParameterValue pv) {
        return !rug.findInvalidParameterValues(new SimpleParameterValues(
                JavaConversions.asScalaBuffer(Collections.singletonList(pv)))).isEmpty();
    }

    private String getPrompt(Parameter parameter, String defaultValue) {
        return String.format("  %s %s %s ", Style.cyan(Constants.DIVIDER),
                Style.yellow(parameter.getName()),
                (defaultValue != null && defaultValue.length() > 0 ? "[" + defaultValue + "]" : "")
                        + (parameter.isRequired() ? "*:" : ":"));
    }

    private ParameterValue readParameter(LineReader console, Parameter parameter,
            String defaultValue) {
        try {
            String value = console.readLine(getPrompt(parameter, defaultValue));

            if (value == null || value.length() == 0) {
                value = defaultValue;
            }

            return new SimpleParameterValue(parameter.getName(), value);
        }
        catch (UserInterruptException e) {
            throw new CommandException("Canceled!");
        }
        catch (EndOfFileException e) {
            throw new CommandException("Canceled!");
        }
    }

    private void validateCollectedParameters(ArtifactDescriptor artifact, ParameterizedRug rug,
            ParameterValues arguments) {
        Collection<ParameterValue> invalid = asJavaCollection(
                rug.findInvalidParameterValues(arguments));
        Collection<Parameter> missing = asJavaCollection(rug.findMissingParameters(arguments));

        if (!invalid.isEmpty() || !missing.isEmpty()) {
            if (!CommandLineOptions.hasOption("interactive")) {
                log.newline();
            }

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
                    StringUtils.stripName(rug.name(), artifact)));
        }
    }

    protected ParameterValues validate(ArtifactDescriptor artifact, ParameterizedRug rug,
            ParameterValues arguments) {
        arguments = collectParameters(rug, arguments);
        arguments = rug.addDefaultParameterValues(arguments);
        validateCollectedParameters(artifact, rug, arguments);
        return arguments;
    }

    protected ParameterValues mergeParameters(ParameterValues arguments, ParameterValue... pv) {
        List<ParameterValue> pvs = new ArrayList<>();
        if (arguments != null) {
            pvs.addAll(asJavaCollection(arguments.parameterValues()));
        }
        if (pv != null) {
            Arrays.stream(pv).forEach(pvs::add);
        }
        return new SimpleParameterValues(asScalaBuffer(pvs));
    }

}
