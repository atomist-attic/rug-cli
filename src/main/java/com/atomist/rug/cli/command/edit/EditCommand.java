package com.atomist.rug.cli.command.edit;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.atomist.project.ProjectOperationArguments;
import com.atomist.project.ProvenanceInfoWriter;
import com.atomist.project.archive.Operations;
import com.atomist.project.edit.FailedModificationAttempt;
import com.atomist.project.edit.ModificationAttempt;
import com.atomist.project.edit.NoModificationNeeded;
import com.atomist.project.edit.ProjectEditor;
import com.atomist.project.edit.SuccessfulModification;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.command.AbstractDeltaHandlingCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import com.atomist.source.Delta;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;

import scala.collection.JavaConverters;

public class EditCommand extends AbstractDeltaHandlingCommand {

    private Log log = new Log(getClass());

    @Command
    public void run(Operations operations, ArtifactDescriptor artifact,
            @Argument(index = 1) String fqArtifactName,
            @Argument(start = 2) ProjectOperationArguments arguments,
            @Option("change-dir") String root, @Option("dry-run") boolean dryRun,
            @Option("repo") boolean repo) {

        String name = CommandUtils.extractRugTypeName(fqArtifactName);
        if (name == null) {
            throw new CommandException("No editor name provided.", "edit");
        }

        String fqName = artifact.group() + "." + artifact.artifact() + "." + name;
        Optional<ProjectEditor> opt = JavaConverters.asJavaCollection(operations.editors())
                .stream().filter(g -> g.name().equals(name)).findFirst();
        if (!opt.isPresent()) {
            // try again with a properly namespaced name
            opt = JavaConverters.asJavaCollection(operations.editors()).stream()
                    .filter(g -> g.name().equals(fqName)).findFirst();
        }

        if (opt.isPresent()) {
            validate(artifact, opt.get(), arguments);
            invoke(artifact, name, opt.get(), arguments, root, dryRun, repo);
        }
        else {
            log.newline();
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Editors"));
            JavaConverters.asJavaCollection(operations.editors()).forEach(
                    e -> log.info(Style.yellow("  %s", StringUtils.stripName(e.name(), artifact))
                            + " (" + e.description() + ")"));
            StringUtils.printClosestMatch(fqName, artifact, operations.editorNames());
            throw new CommandException(
                    String.format("Specified editor %s could not be found in %s:%s:%s",
                            StringUtils.stripName(name, artifact), artifact.group(),
                            artifact.artifact(), artifact.version()));
        }
    }

    private void invoke(ArtifactDescriptor artifact, String name, ProjectEditor editor,
            ProjectOperationArguments arguments, String rootName, boolean dryRun, boolean repo) {

        File root = FileUtils.createProjectRoot(rootName);

        if (repo) {
            isClean(root);
        }

        ArtifactSource source = new FileSystemArtifactSource(
                new SimpleFileSystemArtifactSourceIdentifier(root));

        ModificationAttempt result = new ProgressReportingOperationRunner<ModificationAttempt>(
                String.format("Running editor %s of %s",
                        StringUtils.stripName(editor.name(), artifact),
                        ArtifactDescriptorUtils.coordinates(artifact)))
                                .run(indicator -> editor.modify(source, arguments));

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

            List<Delta> deltas = JavaConverters.asJavaCollection(resultSource.cachedDeltas())
                    .stream().collect(Collectors.toList());

            iterateDeltas(deltas, source, resultSource, root, dryRun);
            if (repo) {
                log.newline();
                commitFiles(editor, arguments, root);
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

    private void commitFiles(ProjectEditor editor, ProjectOperationArguments arguments, File root) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File(root, ".git")).readEnvironment()
                .findGitDir().build()) {
            try (Git git = new Git(repository)) {
                log.info("Committing to git repository at " + git.getRepository().getDirectory());
                git.add().addFilepattern(".").call();
                RevCommit commit = git.commit().setAll(true)
                        .setMessage(String.format("Commit by editor %s\n\n%s", editor.name(),
                                new ProvenanceInfoWriter().write(editor, arguments,
                                        Constants.cliClient())))
                        .setAuthor("Atomist", "cli@atomist.com").call();
                log.info("Committed changes to git repository (%s)", commit.abbreviate(7).name());
            }
        }
        catch (IllegalStateException | IOException | GitAPIException e) {
            throw new RunnerException(e);
        }
    }

    private void isClean(File root) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File(root, ".git")).readEnvironment()
                .findGitDir().build()) {
            try (Git git = new Git(repository)) {
                Status status = git.status().call();
                if (!status.isClean()) {
                    throw new CommandException(String.format(
                            "Working tree at %s not clean. Please commit or stash your changes before running an editor with -R.",
                            root.getAbsolutePath()), "edit");
                }
            }
        }
        catch (IllegalStateException | IOException | GitAPIException e) {
            throw new RunnerException(e);
        }
    }
}
