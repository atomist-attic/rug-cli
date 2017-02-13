package com.atomist.rug.cli.command.edit;

import static scala.collection.JavaConversions.asJavaCollection;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.text.WordUtils;

import com.atomist.project.ProjectOperationArguments;
import com.atomist.project.ProvenanceInfoWriter;
import com.atomist.project.archive.Operations;
import com.atomist.project.edit.FailedModificationAttempt;
import com.atomist.project.edit.ModificationAttempt;
import com.atomist.project.edit.NoModificationNeeded;
import com.atomist.project.edit.ProjectEditor;
import com.atomist.project.edit.SuccessfulModification;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractDeltaHandlingCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.cli.command.utils.GitUtils;
import com.atomist.rug.cli.command.utils.OperationUtils;
import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.kind.core.ChangeLogEntry;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import com.atomist.source.Delta;

import scala.collection.JavaConverters;

public class EditCommand extends AbstractDeltaHandlingCommand {

    @Command
    public void run(Operations operations, ArtifactDescriptor artifact,
            @Argument(index = 1) String fqArtifactName,
            @Argument(start = 2) ProjectOperationArguments arguments,
            @Option("change-dir") String root, @Option("dry-run") boolean dryRun,
            @Option("repo") boolean repo) {

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
            invoke(artifact, name, opt.get(), arguments, root, dryRun, repo);
        }
        else {
            log.newline();
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Editors"));
            asJavaCollection(operations.editors()).forEach(e -> log.info(Style.yellow("  %s",
                    StringUtils.stripName(e.name(), artifact)) + "\n    "
                    + WordUtils.wrap(e.description(), Constants.WRAP_LENGTH, "\n    ", false)));
            StringUtils.printClosestMatch(fqName, artifact, operations.editorNames());
            throw new CommandException(
                    String.format("Specified editor %s could not be found in %s:%s:%s",
                            StringUtils.stripName(name, artifact), artifact.group(),
                            artifact.artifact(), artifact.version()));
        }
    }

    private void invoke(ArtifactDescriptor artifact, String name, ProjectEditor editor,
            ProjectOperationArguments arguments, String rootName, boolean dryRun, boolean commit) {

        File root = FileUtils.createProjectRoot(rootName);

        if (commit) {
            GitUtils.isClean(root);
        }

        ArtifactSource source = ArtifactSourceUtils.createArtifactSource(root);

        ModificationAttempt result = new ProgressReportingOperationRunner<ModificationAttempt>(
                String.format("Running editor %s of %s",
                        StringUtils.stripName(editor.name(), artifact),
                        ArtifactDescriptorUtils.coordinates(artifact))).run(indicator -> {
                            ModificationAttempt r = editor.modify(source, arguments);

                            printLogEntries(indicator, r);

                            return r;
                        });

        if (result instanceof SuccessfulModification) {

            ArtifactSource resultSource = new ProvenanceInfoWriter().write(
                    ((SuccessfulModification) result).result(), editor, arguments,
                    Constants.cliClient());

            log.newline();
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Project"));
            log.info("  %s (%s in %s files)", Style.underline(FileUtils.relativize(root)),
                    FileUtils.sizeOf(root), resultSource.allFiles().size());
            log.newline();
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Changes"));

            List<Delta> deltas = asJavaCollection(resultSource.cachedDeltas()).stream()
                    .collect(Collectors.toList());

            iterateDeltas(deltas, source, resultSource, root, dryRun);
            if (commit) {
                log.newline();
                GitUtils.commitFiles(editor, arguments, root);
            }

            log.newline();
            log.info(Style.green("Successfully edited project %s", root.getName()));
        }
        else if (result instanceof NoModificationNeeded) {
            log.newline();
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Project"));
            log.info("  %s (%s in %s files)", Style.underline(FileUtils.relativize(root)),
                    FileUtils.sizeOf(root), source.allFiles().size());
            log.newline();
            log.info(Style.yellow("Editor made no changes to project %s", root.getName()));
        }
        else if (result instanceof FailedModificationAttempt) {
            log.newline();
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Project"));
            log.info("  %s (%s in %s files)", Style.underline(FileUtils.relativize(root)),
                    FileUtils.sizeOf(root), source.allFiles().size());
            log.newline();
            throw new CommandException(String.format(
                    "Editor failed to make changes to project %s:\n  %s", root.getName(),
                    ((FailedModificationAttempt) result).failureExplanation()));
        }
    }

    private void printLogEntries(ProgressReporter indicator, ModificationAttempt r) {
        if (r instanceof SuccessfulModification) {
            Collection<ChangeLogEntry<ArtifactSource>> logEntries = JavaConverters
                    .asJavaCollectionConverter(((SuccessfulModification) r).changeLogEntries())
                    .asJavaCollection();
            logEntries.forEach(l -> indicator.report("  " + l.comment()));
        }
    }
}
