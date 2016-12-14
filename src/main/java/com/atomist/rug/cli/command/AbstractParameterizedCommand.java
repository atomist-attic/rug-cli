package com.atomist.rug.cli.command;

import java.util.Collection;

import com.atomist.param.Parameter;
import com.atomist.param.ParameterValue;
import com.atomist.project.ProjectOperation;
import com.atomist.project.ProjectOperationArguments;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;

import scala.collection.JavaConverters;

public abstract class AbstractParameterizedCommand extends AbstractAnnotationBasedCommand {

    private Log log = new Log(getClass());

    protected void validate(ArtifactDescriptor artifact, ProjectOperation operation,
            ProjectOperationArguments arguments) {
        Collection<ParameterValue> invalid = JavaConverters
                .asJavaCollection(operation.findInvalidParameterValues(arguments));
        Collection<Parameter> missing = JavaConverters
                .asJavaCollection(operation.findMissingParameters(arguments));

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
                invalid.forEach(p -> log.info("  " + Style.yellow(p.getName()) + " = " + p.getValue()));
            }
            throw new CommandException(
                    String.format("Missing and/or invalid parameters for %s",
                            StringUtils.stripName(operation.name(), artifact)));
        }
    }

}
