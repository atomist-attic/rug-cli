package com.atomist.rug.cli.command.generate;

import static scala.collection.JavaConversions.asJavaCollection;
import static scala.collection.JavaConversions.asScalaBuffer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import com.atomist.param.ParameterValue;
import com.atomist.param.SimpleParameterValue;
import com.atomist.project.ProjectOperationArguments;
import com.atomist.project.ProvenanceInfoWriter;
import com.atomist.project.SimpleProjectOperationArguments;
import com.atomist.project.archive.Operations;
import com.atomist.project.generate.ProjectGenerator;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.RunnerException;
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
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import com.atomist.source.SimpleSourceUpdateInfo;
import com.atomist.source.file.FileSystemArtifactSourceWriter;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;

public class GenerateCommand extends AbstractParameterizedCommand {

    private Log log = new Log(getClass());

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

    private void initializeRepoAndCommitFiles(ProjectGenerator generator,
            ProjectOperationArguments arguments, File root) {
        try (Git git = Git.init().setDirectory(root).call()) {
            log.info("Initialized a new git repository at " + git.getRepository().getDirectory());
            git.add().addFilepattern(".").call();
            RevCommit commit = git.commit().setAll(true)
                    .setMessage(
                            String.format("Initial commit by generator %s\n\n%s", generator.name(),
                                    new ProvenanceInfoWriter().write(generator, arguments,
                                            Constants.cliClient())))
                    .setAuthor("Atomist", "cli@atomist.com").call();
            log.info("Committed initial set of files to git repository (%s)",
                    commit.abbreviate(7).name());
        }
        catch (IllegalStateException | GitAPIException e) {
            throw new RunnerException(e);
        }
    }

    // Question: what is "name" here? how does it relate to project name?
    private void invoke(ArtifactDescriptor artifact, String name, ProjectGenerator generator,
            ProjectOperationArguments arguments, String rootName, boolean createRepo,
            boolean overwrite) {

        String projectName = arguments.stringParamValue("project_name");
        File root = createProjectRoot(rootName, projectName, overwrite);

        ArtifactSource result = new ProgressReportingOperationRunner<ArtifactSource>(
                String.format("Running generator %s of %s", generator.name(),
                        ArtifactDescriptorUtils.coordinates(artifact)))
                                .run(indicator -> generator.(projectName, arguments));

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
            initializeRepoAndCommitFiles(generator, arguments, root);
        }
        log.newline();
        log.info(Style.green("Successfully generated new project %s", projectName));

    }

    private ProjectOperationArguments mergeParameters(ProjectOperationArguments arguments,
            ParameterValue... pv) {
        List<ParameterValue> pvs = new ArrayList<>();
        if (arguments != null) {
            pvs.addAll(asJavaCollection(arguments.parameterValues()));
        }
        if (pv != null) {
            Arrays.stream(pv).forEach(pvs::add);
        }
        return new SimpleProjectOperationArguments(
                (arguments != null ? arguments.name() : "parameter"), asScalaBuffer(pvs));
    }
}
