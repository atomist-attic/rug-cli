package com.atomist.rug.cli.command.edit;

import static scala.collection.JavaConversions.asJavaCollection;

import java.io.File;
import java.util.Optional;

import org.apache.commons.lang3.text.WordUtils;

import com.atomist.param.ParameterValues;
import com.atomist.project.archive.RugResolver;
import com.atomist.project.archive.Rugs;
import com.atomist.project.edit.ProjectEditor;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractParameterizedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.annotation.Validator;
import com.atomist.rug.cli.command.utils.GitUtils;
import com.atomist.rug.cli.command.utils.LocalGitProjectManagement;
import com.atomist.rug.cli.command.utils.OperationUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;

public class EditCommand extends AbstractParameterizedCommand {

    @Validator
    public void validate(@Argument(index = 1) String fqArtifactName,
            @Option("change-dir") String projectName, @Option("repo") boolean repo) {
        String name = OperationUtils.extractRugTypeName(fqArtifactName);
        if (name == null) {
            throw new CommandException("No editor name provided.", "edit");
        }
        File root = FileUtils.createProjectRoot(projectName);
        if (!root.exists()) {
            throw new CommandException(String.format(
                    "Target directory %s does not exist.\nPlease fix the directory path provided to --change-dir.",
                    projectName), "edit");
        }
        if (!root.isDirectory()) {
            throw new CommandException(String.format(
                    "Target path %s is not a directory.\nPlease fix the directory path provided to --change-dir.",
                    projectName), "edit");
        }
        if (repo) {
            GitUtils.isClean(root, "edit");
        }
    }

    @Command
    public void run(Rugs operations, ArtifactDescriptor artifact,
            @Argument(index = 1) String fqArtifactName,
            @Argument(start = 2) ParameterValues arguments, @Option("change-dir") String root,
            @Option("dry-run") boolean dryRun, @Option("repo") boolean repo, RugResolver resolver) {

        String editorName = OperationUtils.extractRugTypeName(fqArtifactName);
        Optional<ProjectEditor> opt = asJavaCollection(operations.editors()).stream()
                .filter(g -> g.name().equals(editorName)).findFirst();

        if (opt.isPresent()) {
            arguments = validate(artifact, opt.get(), arguments);
            invoke(artifact, opt.get(), arguments, root, dryRun, repo, resolver);
        }
        else {
            if (!operations.editors().isEmpty()) {
                log.newline();
                log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Editors"));
                asJavaCollection(operations.editors()).forEach(e -> log.info(
                        Style.yellow("  %s", StringUtils.stripName(e.name(), artifact)) + "\n    "
                                + WordUtils.wrap(
                                        org.apache.commons.lang3.StringUtils
                                                .capitalize(e.description()),
                                        Constants.WRAP_LENGTH, "\n    ", false)));
                StringUtils.printClosestMatch(editorName, artifact, operations.editorNames());
            }
            throw new CommandException(
                    String.format("Specified editor %s could not be found in %s:%s",
                            StringUtils.stripName(editorName, artifact), artifact.group(),
                            artifact.artifact()));
        }
    }

    private void invoke(ArtifactDescriptor artifact, ProjectEditor editor,
            ParameterValues arguments, String rootName, boolean dryRun, boolean commit,
            RugResolver resolver) {

        LocalGitProjectManagement management = new LocalGitProjectManagement(artifact, rootName,
                false, false, commit, dryRun, resolver);
        management.edit(editor, arguments, rootName);
    }
}
