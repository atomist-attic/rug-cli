package com.atomist.rug.cli.command.edit;

import com.atomist.param.ParameterValues;
import com.atomist.project.archive.Rugs;
import com.atomist.project.edit.ProjectEditor;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractParameterizedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.utils.OperationUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.LocalGitProjectManagement;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Optional;

import static scala.collection.JavaConversions.asJavaCollection;

public class EditCommand extends AbstractParameterizedCommand {

    @Command
    public void run(Rugs operations, ArtifactDescriptor artifact,
            @Argument(index = 1) String fqArtifactName,
            @Argument(start = 2) ParameterValues arguments, @Option("change-dir") String root,
            @Option("dry-run") boolean dryRun, @Option("repo") boolean repo) {

        String name = OperationUtils.extractRugTypeName(fqArtifactName);
        if (name == null) {
            throw new CommandException("No editor name provided.", "edit");
        }

        String fqName = artifact.group() + "." + artifact.artifact() + "." + name;
        Optional<ProjectEditor> opt = asJavaCollection(operations.editors()).stream()
                .filter(g -> g.name().equals(name)).findFirst();
        if (!opt.isPresent()) {
            // try again with a properly namespaced name
            opt = asJavaCollection(operations.editors()).stream()
                    .filter(g -> g.name().equals(fqName)).findFirst();
        }

        if (opt.isPresent()) {
            arguments = validate(artifact, opt.get(), arguments);
            invoke(artifact, opt.get(), arguments, root, dryRun, repo);
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
                StringUtils.printClosestMatch(fqName, artifact, operations.editorNames());
            }
            throw new CommandException(
                    String.format("Specified editor %s could not be found in %s:%s:%s",
                            StringUtils.stripName(name, artifact), artifact.group(),
                            artifact.artifact(), artifact.version()));
        }
    }

    private void invoke(ArtifactDescriptor artifact, ProjectEditor editor,
            ParameterValues arguments, String rootName, boolean dryRun, boolean commit) {

        LocalGitProjectManagement management = new LocalGitProjectManagement(artifact, rootName,
                false, false, commit, dryRun);
        management.edit(editor, arguments, rootName);
    }
}
