package com.atomist.rug.cli.command.generate;

import static scala.collection.JavaConversions.asJavaCollection;

import java.util.Optional;

import org.apache.commons.lang3.text.WordUtils;

import com.atomist.param.Parameter;
import com.atomist.param.ParameterValues;
import com.atomist.param.SimpleParameterValue;
import com.atomist.project.archive.Rugs;
import com.atomist.project.generate.ProjectGenerator;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractParameterizedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.annotation.Validator;
import com.atomist.rug.cli.command.utils.LocalGitProjectManagement;
import com.atomist.rug.cli.command.utils.OperationUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;

import scala.collection.JavaConverters;

public class GenerateCommand extends AbstractParameterizedCommand {

    @Validator
    public void validate(@Argument(index = 1) String fqArtifactName,
            @Option("change-dir") String path) {
        String generatorName = OperationUtils.extractRugTypeName(fqArtifactName);
        if (generatorName == null) {
            throw new CommandException("No generator name provided.", "generate");
        }
    }

    @Command
    public void run(Rugs rugs, ArtifactDescriptor artifact,
            @Argument(index = 1) String fqArtifactName, @Argument(index = 2) String projectName,
            @Argument(start = 3) ParameterValues arguments, @Option("change-dir") String root,
            @Option("repo") boolean createRepo, @Option("overwrite") boolean overwrite) {

        if (projectName != null) {
            arguments = mergeParameters(arguments,
                    new SimpleParameterValue("project_name", projectName));
        }

        String generatorName = OperationUtils.extractRugTypeName(fqArtifactName);
        Optional<ProjectGenerator> opt = asJavaCollection(rugs.generators()).stream()
                .filter(g -> g.name().equals(generatorName)).findFirst();
        if (opt.isPresent()) {
            arguments = validate(artifact, opt.get(), arguments);
            invoke(artifact, opt.get(), arguments, root, createRepo, overwrite);
        }
        else {
            if (rugs.generators().nonEmpty()) {
                log.newline();
                log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Generators"));
                asJavaCollection(rugs.generators())
                        .forEach(e -> log.info(Style.yellow("  %s", e.name()) + "\n    "
                                + WordUtils.wrap(
                                        org.apache.commons.lang3.StringUtils
                                                .capitalize(e.description()),
                                        Constants.WRAP_LENGTH, "\n    ", false)));
                StringUtils.printClosestMatch(generatorName, artifact, rugs.generatorNames());
            }
            throw new CommandException(String.format(
                    "Specified generator %s could not be found in %s:%s", generatorName,
                    artifact.group(), artifact.artifact()));
        }
    }

    private void invoke(ArtifactDescriptor artifact, ProjectGenerator generator,
            ParameterValues arguments, String rootName, boolean createRepo, boolean overwrite) {

        String projectName = projectName(generator, arguments);

        LocalGitProjectManagement manager = new LocalGitProjectManagement(artifact, rootName,
                createRepo, overwrite, false, false);
        manager.generate(generator, arguments, projectName);
    }

    private String projectName(ProjectGenerator generator, ParameterValues arguments) {
        if (JavaConverters.mapAsJavaMapConverter(arguments.parameterValueMap()).asJava()
                .containsKey("project_name")) {
            return arguments.stringParamValue("project_name");
        }
        // extract potential default project_name
        else {
            Optional<Parameter> projectNameParameter = JavaConverters
                    .asJavaCollectionConverter(generator.parameters()).asJavaCollection().stream()
                    .filter(p -> p.getName().equals("project_name")).findAny();
            if (projectNameParameter.isPresent() && projectNameParameter.get().hasDefaultValue()) {
                return projectNameParameter.get().getDefaultValue();
            }
        }
        throw new CommandException("No PROJECT_NAME provided", "generate");
    }
}
