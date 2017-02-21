package com.atomist.rug.cli.command;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.source.ArtifactSource;
import com.atomist.source.ByteArrayFileArtifact;
import com.atomist.source.Delta;
import com.atomist.source.FileAdditionDelta;
import com.atomist.source.FileArtifact;
import com.atomist.source.FileDeletionDelta;
import com.atomist.source.FileUpdateDelta;
import com.atomist.source.file.FileSystemArtifactSourceWriter;

import difflib.DiffUtils;

public abstract class AbstractDeltaHandlingCommand extends AbstractParameterizedCommand {

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

}