package com.atomist.rug.cli.command.generate;

import static scala.collection.JavaConversions.asJavaCollection;

import java.io.File;
import java.util.Optional;

import org.apache.commons.lang3.text.WordUtils;

import com.atomist.param.Parameter;
import com.atomist.param.SimpleParameterValue;
import com.atomist.project.ProjectOperationArguments;
import com.atomist.project.ProvenanceInfoWriter;
import com.atomist.project.archive.Operations;
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
import com.atomist.rug.cli.utils.GitUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import com.atomist.source.SimpleSourceUpdateInfo;
import com.atomist.source.file.FileSystemArtifactSourceWriter;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;

import scala.collection.JavaConverters;

public class GenerateCommand extends AbstractParameterizedCommand {

    @Command
    public void run(Operations operations, ArtifactDescriptor artifact,
            @Argument(index = 1) String fqArtifactName, @Argument(index = 2) String projectName,
            @Argument(start = 3) ProjectOperationArguments arguments,
            @Option("change-dir") String root, @Option("repo") boolean createRepo,
            @Option("overwrite") boolean overwrite) {

        if (projectName != null) {
            arguments = mergeParameters(arguments,
                    new SimpleParameterValue("project_name", projectName));
        }

        String name = OperationUtils.extractRugTypeName(fqArtifactName);
        if (name == null) {
            throw new CommandException("No generator name provided.", "generate");
        }

        Optional<ProjectGenerator> opt = asJavaCollection(operations.generators()).stream()
                .filter(g -> g.name().equals(name)).findFirst();
        if (opt.isPresent()) {
            arguments = validate(artifact, opt.get(), arguments);
            invoke(artifact, name, opt.get(), arguments, root, createRepo, overwrite);
        }
        else {
            log.newline();
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Generators"));
            asJavaCollection(operations.generators())
                    .forEach(e -> log.info(Style.yellow("  %s", e.name()) + "\n    " + WordUtils
                            .wrap(e.description(), Constants.WRAP_LENGTH, "\n    ", false)));
            StringUtils.printClosestMatch(name, artifact, operations.generatorNames());
            throw new CommandException(
                    String.format("Specified generator %s could not be found in %s:%s:%s", name,
                            artifact.group(), artifact.artifact(), artifact.version()));
        }
    }

    private File createProjectRoot(String path, String projectName, boolean overwrite) {
        path = FileUtils.createProjectRoot(path).getAbsolutePath();

        File root = new File(path + File.separator + projectName);
        if (root.exists() && !overwrite) {
            throw new CommandException(String.format(
                    "Target directory %s already exists. Specify -F to overwrite existing content.",
                    root.getAbsolutePath().toString()), "generate");
        }
        if (!root.getParentFile().exists()) {
            root.getParentFile().mkdirs();
        }
        return root;
    }

    private void invoke(ArtifactDescriptor artifact, String name, ProjectGenerator generator,
            ProjectOperationArguments arguments, String rootName, boolean createRepo,
            boolean overwrite) {

        String projectName = projectName(generator, arguments);

        File root = createProjectRoot(rootName, projectName, overwrite);

        ArtifactSource result = new ProgressReportingOperationRunner<ArtifactSource>(
                String.format("Running generator %s of %s", generator.name(),
                        ArtifactDescriptorUtils.coordinates(artifact)))
                                .run(indicator -> generator.generate(arguments));

        // Add provenance info to output
        result = new ProvenanceInfoWriter().write(result, generator, arguments,
                Constants.cliClient());

        new FileSystemArtifactSourceWriter().write(result,
                new SimpleFileSystemArtifactSourceIdentifier(root),
                new SimpleSourceUpdateInfo(name));

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Project"));
        log.info("  %s (%s in %s files)", Style.underline(FileUtils.relativize(root)),
                FileUtils.sizeOf(root), result.allFiles().size());
        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Changes"));
        ArtifactSourceTreeCreator.visitTree(result, new LogVisitor(log));
        if (createRepo) {
            log.newline();
            GitUtils.initializeRepoAndCommitFiles(generator, arguments, root);
        }
        log.newline();
        log.info(Style.green("Successfully generated new project %s", projectName));

    }

    private String projectName(ProjectGenerator generator, ProjectOperationArguments arguments) {
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
