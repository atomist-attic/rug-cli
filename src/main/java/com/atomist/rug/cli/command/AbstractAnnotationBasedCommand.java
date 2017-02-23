package com.atomist.rug.cli.command;

import com.atomist.param.ParameterValue;
import com.atomist.param.ParameterValues;
import com.atomist.param.SimpleParameterValue;
import com.atomist.param.SimpleParameterValues;
import com.atomist.project.archive.Rugs;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import org.apache.commons.cli.CommandLine;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static scala.collection.JavaConversions.asScalaBuffer;

public abstract class AbstractAnnotationBasedCommand
        extends AbstractCompilingAndOperationLoadingCommand {

    private <A extends Annotation> Optional<Method> annotatedMethodWith(Class<A> annotationClass) {
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(getClass()))
                .filter(m -> AnnotationUtils.getAnnotation(m, annotationClass) != null).findFirst();
    }

    private void invokeMethod(Method method, List<Object> arguments) {
        try {
            method.invoke(this, (Object[]) arguments.toArray(new Object[arguments.size()]));
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RunnerException(e);
        }
    }

    private Object prepareArgumentMethodArgument(CommandLine commandLine, Parameter p,
            Argument argument) {
        if (argument.start() != -1) {
            if (p.getType().equals(ParameterValues.class)) {
                List<ParameterValue> pvs = new ArrayList<>();
                if (argument.start() < commandLine.getArgList().size()) {

                    for (int ix = argument.start(); ix < commandLine.getArgList().size(); ix++) {
                        String arg = commandLine.getArgList().get(ix);
                        int i = arg.indexOf('=');
                        String name;
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
                return new SimpleParameterValues(asScalaBuffer(pvs));
            }
        }
        String value = null;
        if (argument.start() == -1 && argument.index() < commandLine.getArgList().size()) {
            value = commandLine.getArgList().get(argument.index());
        }
        if (value == null) {
            value = (argument.defaultValue().equals(Argument.DEFAULT_NONE) ? null
                    : argument.defaultValue());
        }
        value = StringUtils.expandEnvironmentVars(value);
        return convert(p.getType(), value);
    }

    private ParameterValues prepareArguments(Properties props) {
        List<ParameterValue> pvs = props.entrySet().stream()
                .map(e -> new SimpleParameterValue((String) e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return new SimpleParameterValues(asScalaBuffer(pvs));
    }

    private List<Object> prepareMethodArguments(Method method, Rugs rugs,
            ArtifactDescriptor artifact, ArtifactSource source, CommandLine commandLine) {

        return Arrays.stream(method.getParameters()).map(p -> {
            Argument argument = AnnotationUtils.getAnnotation(p, Argument.class);
            Option option = AnnotationUtils.getAnnotation(p, Option.class);

            if (argument != null) {
                return prepareArgumentMethodArgument(commandLine, p, argument);
            }
            else if (option != null) {
                return prepareOptionMethodArgument(commandLine, p, option);
            }
            else if (p.getType().equals(Rugs.class)) {
                return rugs;
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
                return SettingsReader.read();
            }
            return null;
        }).collect(Collectors.toList());
    }

    private Object prepareOptionMethodArgument(CommandLine commandLine, Parameter p,
            Option option) {
        if (p.getType().equals(boolean.class)) {
            return commandLine.hasOption(option.value());
        }
        else if (p.getType().equals(Properties.class)) {
            return commandLine.getOptionProperties(option.value());
        }
        else if (p.getType().equals(ParameterValues.class)) {
            return prepareArguments(commandLine.getOptionProperties(option.value()));
        }
        else {
            String value = commandLine.getOptionValue(option.value());
            if (value == null) {
                value = (option.defaultValue().equals(Argument.DEFAULT_NONE) ? null
                        : option.defaultValue());
            }
            value = StringUtils.expandEnvironmentVars(value);
            return convert(p.getType(), value);
        }
    }

    private Object convert(Class<?> cls, String value) {
        if (Integer.class.equals(cls) || int.class.equals(cls)) {
            try {
                return Integer.valueOf(value);
            }
            catch (NumberFormatException e) {
                throw new CommandException(String.format("Provided option or argument value %s is not a valid number.", value),
                        registry.findCommand(getClass()).name());
            }
        }
        else if (Boolean.class.equals(cls) || boolean.class.equals(cls)) {
            return Boolean.valueOf(value);
        }
        return value;
    }
    
    @Override
    protected void validate(ArtifactDescriptor artifact, CommandLine commandLine) {
        Optional<Method> validatorMethod = annotatedMethodWith(
                com.atomist.rug.cli.command.annotation.Validator.class);
        if (validatorMethod.isPresent()) {
            List<Object> validatorArgs = prepareMethodArguments(validatorMethod.get(), null,
                    artifact, null, commandLine);
            invokeMethod(validatorMethod.get(), validatorArgs);
        }
    }

    @Override
    protected final void run(Rugs rugs, ArtifactDescriptor artifact, ArtifactSource source,
            CommandLine commandLine) {

        Optional<Method> commandMethod = annotatedMethodWith(
                com.atomist.rug.cli.command.annotation.Command.class);

        if (commandMethod.isPresent()) {
            List<Object> runArgs = prepareMethodArguments(commandMethod.get(), rugs, artifact,

                    source, commandLine);
            invokeMethod(commandMethod.get(), runArgs);
        }
        else {
            throw new CommandException("Command class does not have an @Command-annotated method.");
        }
    }
}
