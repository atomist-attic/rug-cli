package com.atomist.rug.cli.command;

import static scala.collection.JavaConversions.asScalaBuffer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import com.atomist.param.ParameterValue;
import com.atomist.param.SimpleParameterValue;
import com.atomist.project.ProjectOperationArguments;
import com.atomist.project.SimpleProjectOperationArguments;
import com.atomist.project.archive.Operations;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.loader.Handlers;
import com.atomist.rug.loader.OperationsAndHandlers;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;

public abstract class AbstractAnnotationBasedCommand
        extends AbstractCompilingAndOperationLoadingCommand {

    @Override
    protected void run(OperationsAndHandlers operations, ArtifactDescriptor artifact,
            ArtifactSource source, CommandLine commandLine) {

        Optional<Method> methodOptional = Arrays.stream(ReflectionUtils.getAllDeclaredMethods(getClass()))
                                                .filter(m -> AnnotationUtils.getAnnotation(m,
                        com.atomist.rug.cli.command.annotation.Command.class) != null)
                                                .findFirst();

        if (methodOptional.isPresent()) {
            invokeCommandMethod(methodOptional.get(), operations, artifact, source, commandLine);
        }
        else {
            throw new CommandException("Command class does not have an @Command-annotated method.");
        }
    }

    protected void invokeCommandMethod(Method method, OperationsAndHandlers operations,
            ArtifactDescriptor artifact, ArtifactSource source, CommandLine commandLine) {
        List<Object> arguments = prepareMethodArguments(method, operations, artifact, source,
                commandLine);
        try {
            method.invoke(this, (Object[]) arguments.toArray(new Object[arguments.size()]));
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RunnerException(e);
        }
    }

    private ProjectOperationArguments prepareArguments(Properties props, String name) {
        List<ParameterValue> pvs = props.entrySet().stream()
                .map(e -> new SimpleParameterValue((String) e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return new SimpleProjectOperationArguments(name, asScalaBuffer(pvs));
    }

    private Object prepareArgumentMethodArgument(CommandLine commandLine, Parameter p,
            Argument argument) {
        Object argumentValue = null;
        if (argument.start() != -1) {
            if (p.getType().equals(ProjectOperationArguments.class)) {
                List<ParameterValue> pvs = new ArrayList<>();
                if (argument.start() < commandLine.getArgList().size()) {

                    for (int ix = argument.start(); ix < commandLine.getArgList().size(); ix++) {
                        String arg = commandLine.getArgList().get(ix);
                        int i = arg.indexOf('=');
                        String name = null;
                        String value = null;
                        if (i < 0) {
                            name = arg;
                        }
                        else {
                            name = arg.substring(0, i);
                            value = arg.substring(i + 1);
                        }
                        pvs.add(new SimpleParameterValue(name, value));
                    }
                }
                argumentValue = new SimpleProjectOperationArguments("parameter",
                        asScalaBuffer(pvs));
            }
        }
        else if (argument.start() == -1 && argument.index() < commandLine.getArgList().size()) {
            argumentValue = commandLine.getArgList().get(argument.index());
        }
        if (argumentValue == null) {
            argumentValue = (argument.defaultValue().equals(Argument.DEFAULT_NONE) ? null
                    : argument.defaultValue());
        }
        return argumentValue;
    }

    private List<Object> prepareMethodArguments(Method method, OperationsAndHandlers operations,
            ArtifactDescriptor artifact, ArtifactSource source, CommandLine commandLine) {

        List<Object> arguments = Arrays.stream(method.getParameters()).map(p -> {

            Argument argument = AnnotationUtils.getAnnotation(p, Argument.class);
            Option option = AnnotationUtils.getAnnotation(p, Option.class);

            if (argument != null) {
                return prepareArgumentMethodArgument(commandLine, p, argument);
            }
            else if (option != null) {
                return prepareOptionMethodArgument(commandLine, p, option);
            }
            else if (p.getType().equals(Operations.class)) {
                return operations.operations();
            }
            else if (p.getType().equals(Handlers.class)) {
                return operations.handlers();
            }
            else if (p.getType().equals(OperationsAndHandlers.class)) {
                return operations;
            }
            else if (p.getType().equals(ArtifactDescriptor.class)) {
                return artifact;
            }
            else if (p.getType().equals(ArtifactSource.class)) {
                return source;
            }
            else if (p.getType().equals(CommandLine.class)) {
                return commandLine;
            }
            else if (p.getType().equals(Settings.class)) {
                return new SettingsReader().read();
            }
            return null;
        }).collect(Collectors.toList());

        return arguments;
    }

    private Object prepareOptionMethodArgument(CommandLine commandLine, Parameter p,
            Option option) {
        if (p.getType().equals(boolean.class)) {
            return commandLine.hasOption(option.value());
        }
        else if (p.getType().equals(Properties.class)) {
            return commandLine.getOptionProperties(option.value());
        }
        else if (p.getType().equals(ProjectOperationArguments.class)) {
            return prepareArguments(commandLine.getOptionProperties(option.value()),
                    option.value());
        }
        else {
            return StringUtils.expandEnvironmentVars(commandLine.getOptionValue(option.value()));
        }
    }
}
