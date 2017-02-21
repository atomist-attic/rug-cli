package com.atomist.rug.cli.utils;

import com.atomist.param.ParameterValues;
import com.atomist.project.edit.FailedModificationAttempt;
import com.atomist.project.edit.ModificationAttempt;
import com.atomist.project.edit.NoModificationNeeded;
import com.atomist.project.edit.ProjectEditor;
import com.atomist.project.edit.SuccessfulModification;
import com.atomist.project.generate.ProjectGenerator;
import com.atomist.project.review.ProjectReviewer;
import com.atomist.project.review.ReviewResult;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.cli.command.utils.GitUtils;
import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.tree.ArtifactSourceTreeCreator;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.kind.core.ChangeLogEntry;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.project.ProvenanceInfoWriter;
import com.atomist.rug.runtime.plans.ProjectManagement;
import com.atomist.source.ArtifactSource;
import com.atomist.source.ByteArrayFileArtifact;
import com.atomist.source.Delta;
import com.atomist.source.FileAdditionDelta;
import com.atomist.source.FileArtifact;
import com.atomist.source.FileDeletionDelta;
import com.atomist.source.FileUpdateDelta;
import com.atomist.source.SimpleSourceUpdateInfo;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.FileSystemArtifactSourceIdentifier;
import com.atomist.source.file.FileSystemArtifactSourceWriter;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;
import difflib.DiffUtils;
import scala.collection.JavaConverters;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static scala.collection.JavaConversions.asJavaCollection;

/**
 * Save artifact sources in local git
 */
public class LocalGitProjectManagement implements ProjectManagement {
    
    private static final Log log = new Log(LocalGitProjectManagement.class);
    
    private final String rootPath;
    private final boolean createRepo;
    private final boolean overwrite;
    private final ArtifactDescriptor artifact;
    private final boolean commit;
    private final boolean dryRun;

    public LocalGitProjectManagement(ArtifactDescriptor artifact, String rootPath,
            boolean createRepo, boolean overwrite, boolean commit, boolean dryRun) {
        this.artifact = artifact;
        this.rootPath = rootPath;
        this.createRepo = createRepo;
        this.overwrite = overwrite;
        this.commit = commit;
        this.dryRun = dryRun;
    }

    private File createProjectRoot(String path, String projectName, boolean overwrite) {
        path = FileUtils.createProjectRoot(path).getAbsolutePath();

        File root = new File(path + File.separator + projectName);
        if (root.exists() && !overwrite) {
            throw new CommandException(String.format(
                    "Target directory %s already exists.\nSpecify -F to overwrite existing content.",
                    root.getAbsolutePath()), "generate");
        }
        if (!root.getParentFile().exists()) {
            root.getParentFile().mkdirs();

        }
        return root;
    }

    @Override
    public ArtifactSource generate(ProjectGenerator generator, ParameterValues arguments,
            String projectName) {
        final File root = createProjectRoot(rootPath, projectName, overwrite);

        ArtifactSource result = new ProgressReportingOperationRunner<ArtifactSource>(
                String.format("Running generator %s of %s", generator.name(),
                        ArtifactDescriptorUtils.coordinates(artifact)))
                                .run(indicator -> generator.generate(projectName, arguments));

        // Add provenance info to output
        result = new ProvenanceInfoWriter().write(result, generator, arguments,
                Constants.cliClient());

        FileSystemArtifactSourceIdentifier fsid = new SimpleFileSystemArtifactSourceIdentifier(
                root);

        File resultFile = new FileSystemArtifactSourceWriter().write(result, fsid,
                new SimpleSourceUpdateInfo(generator.name()));

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Project"));
        log.info("  %s (%s in %s files)", Style.underline(FileUtils.relativize(resultFile)),
                FileUtils.sizeOf(resultFile), result.allFiles().size());
        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Changes"));
        ArtifactSourceTreeCreator.visitTree(result, new LogVisitor(log));
        if (createRepo) {
            log.newline();
            GitUtils.initializeRepoAndCommitFiles(generator, arguments, root);
        }
        log.newline();
        log.info(Style.green("Successfully generated new project %s", projectName));

        return new FileSystemArtifactSource(fsid);
    }

    @Override
    public ModificationAttempt edit(ProjectEditor editor, ParameterValues arguments,
            String projectName) {
        File root = FileUtils.createProjectRoot(projectName);

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
        return result;
    }

    private void printLogEntries(ProgressReporter indicator, ModificationAttempt r) {
        if (r instanceof SuccessfulModification) {
            Collection<ChangeLogEntry<ArtifactSource>> logEntries = JavaConverters
                    .asJavaCollectionConverter(((SuccessfulModification) r).changeLogEntries())
                    .asJavaCollection();
            logEntries.forEach(l -> indicator.report("  " + l.comment()));
        }
    }

    protected void iterateDeltas(Collection<Delta> deltas, ArtifactSource source,
            ArtifactSource resultSource, File root, boolean dryRun) {
        FileSystemArtifactSourceWriter writer = new FileSystemArtifactSourceWriter();
        Delta lastDelta = deltas.stream().reduce((d1, d2) -> d2).orElse(null);

        deltas.forEach(d -> {
            if (d instanceof FileAdditionDelta) {
                FileAdditionDelta delta = ((FileAdditionDelta) d);
                if (!dryRun) {
                    writer.write(delta.newFile(), root);
                    logOperation("created", null, delta.newFile().path(), root,
                            delta.equals(lastDelta));
                }
                else if (!(delta.newFile() instanceof ByteArrayFileArtifact)) {
                    scala.Option<FileArtifact> opt = source.findFile(delta.newFile().path());
                    String existingContent = "";
                    if (opt.isDefined()) {
                        existingContent = opt.get().content();
                    }
                    String newContent = delta.newFile().content();
                    logPatch(delta.newFile().path(), delta.newFile().path(), existingContent,
                            newContent);
                }
            }
            else if (d instanceof FileUpdateDelta) {
                FileUpdateDelta delta = ((FileUpdateDelta) d);
                if (!dryRun) {
                    File file = new File(root, delta.oldFile().path());
                    file.delete();
                    writer.write(delta.updatedFile(), root);
                    logOperation("updated", delta.oldFile().path(), delta.updatedFile().path(),
                            root, delta.equals(lastDelta));
                }
                else if (!(delta.updatedFile() instanceof ByteArrayFileArtifact)) {
                    scala.Option<FileArtifact> opt = source.findFile(delta.updatedFile().path());
                    String existingContent = "";
                    if (opt.isDefined()) {
                        existingContent = opt.get().content();
                    }
                    String newContent = delta.updatedFile().content();
                    logPatch(delta.path(), delta.updatedFile().path(), existingContent, newContent);
                }
            }
            else if (d instanceof FileDeletionDelta) {
                FileDeletionDelta delta = (FileDeletionDelta) d;
                if (!dryRun) {
                    File file = new File(root, d.path());
                    file.delete();
                    logOperation("deleted", d.path(), null, root, delta.equals(lastDelta));
                }
                else if (!(delta.oldFile() instanceof ByteArrayFileArtifact)) {
                    scala.Option<FileArtifact> opt = source.findFile(delta.oldFile().path());
                    String existingContent = "";
                    if (opt.isDefined()) {
                        existingContent = opt.get().content();
                    }
                    String newContent = "";
                    logPatch(delta.oldFile().path(), "", existingContent, newContent);
                }
            }
        });
    }

    protected void logOperation(String operation, String oldPath, String newPath, File root,
            boolean last) {
        oldPath = (oldPath == null ? "" : oldPath);
        newPath = (newPath == null ? "" : newPath);

        StringBuilder sb = new StringBuilder();
        if (last) {
            sb.append("  ").append(Constants.LAST_TREE_NODE);
        }
        else {
            sb.append("  ").append(Constants.TREE_NODE);
        }
        if (!oldPath.equals("") && !newPath.equals("") && !oldPath.equals(newPath)) {
            sb.append(Style.yellow(oldPath)).append(" ").append(Constants.DIVIDER).append(" ")
                    .append(Style.yellow(newPath)).append(" ").append(operation);
        }
        else if (!"".equals(newPath)) {
            sb.append(Style.yellow(newPath)).append(" ").append(operation);
        }
        else {
            sb.append(Style.yellow(oldPath)).append(" ").append(operation);
        }
        if (root != null) {
            File file = new File(root, newPath);
            sb.append(" (").append(FileUtils.sizeOf(file)).append(")");
        }

        log.info(sb.toString());
    }

    protected void logPatch(String oldPath, String newPath, String existingContent,
            String newContent) {
        difflib.Patch<String> patch = DiffUtils.diff(Arrays.asList(existingContent.split("\n")),
                Arrays.asList(newContent.split("\n")));
        List<String> diffs = DiffUtils.generateUnifiedDiff(oldPath, newPath,
                Arrays.asList(existingContent.split("\n")), patch, 2);
        diffs.forEach(diff -> {
            if (diff.startsWith("+")) {
                log.info("  " + Style.green(diff));
            }
            else if (diff.startsWith("-")) {
                log.info("  " + Style.red(diff));
            }
            else if (diff.startsWith("@@")) {
                log.info("  " + Style.cyan(diff));
            }
            else {
                log.info("  " + diff);
            }
        });
    }

    @Override
    public ReviewResult review(ProjectReviewer reviewer, ParameterValues arguments,
            String projectName) {
        return null;
    }
}
