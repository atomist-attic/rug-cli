package com.atomist.rug.cli.command.generate;

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
import com.atomist.rug.cli.command.utils.OperationUtils;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.tree.ArtifactSourceTreeCreator;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.LocalGitProjectPersister;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import com.atomist.source.file.FileSystemArtifactSource;
import org.apache.commons.lang3.text.WordUtils;
import scala.collection.JavaConverters;

import java.util.Optional;

import static scala.collection.JavaConversions.asJavaCollection;

public class GenerateCommand extends AbstractParameterizedCommand {

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
        if (generatorName == null) {
            throw new CommandException("No generator name provided.", "generate");
        }

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
        }
    }

    private void invoke(ArtifactDescriptor artifact,
            ProjectGenerator generator, ParameterValues arguments, String rootName,
            boolean createRepo, boolean overwrite) {

        String projectName = projectName(generator, arguments);

        LocalGitProjectPersister persister = new LocalGitProjectPersister(rootName, createRepo, overwrite);

        ArtifactSource result = new ProgressReportingOperationRunner<ArtifactSource>(
                String.format("Running generator %s of %s", generator.name(),
                        ArtifactDescriptorUtils.coordinates(artifact)))
                                .run(indicator -> generator.generate(projectName, arguments));

        FileSystemArtifactSource output = persister.persist(generator, arguments, projectName, result);

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Project"));
        log.info("  %s (%s in %s files)", Style.underline(FileUtils.relativize(output.id().rootFile())),
                FileUtils.sizeOf(output.id().rootFile()), result.allFiles().size());
        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Changes"));
        ArtifactSourceTreeCreator.visitTree(result, new LogVisitor(log));
        if (createRepo) {
            log.newline();
        }
        log.newline();
        log.info(Style.green("Successfully generated new project %s", projectName));


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
