package com.atomist.rug.cli.utils;

import com.atomist.param.ParameterValues;
import com.atomist.project.edit.ModificationAttempt;
import com.atomist.project.edit.ProjectEditor;
import com.atomist.project.generate.ProjectGenerator;
import com.atomist.project.review.ProjectReviewer;
import com.atomist.project.review.ReviewResult;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.utils.GitUtils;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.tree.ArtifactSourceTreeCreator;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.project.ProvenanceInfoWriter;
import com.atomist.rug.runtime.plans.ProjectManagement;
import com.atomist.source.ArtifactSource;
import com.atomist.source.SimpleSourceUpdateInfo;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.FileSystemArtifactSourceIdentifier;
import com.atomist.source.file.FileSystemArtifactSourceWriter;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;

import java.io.File;

/**
 * Save artifact sources in local git
 */
public class LocalGitProjectManagement implements ProjectManagement {

    private final String rootPath;
    private final boolean createRepo;
    private final boolean overwrite;
    private final ArtifactDescriptor artifact;
    private final Log log;

    public LocalGitProjectManagement(ArtifactDescriptor artifact, Log log, String rootPath, boolean createRepo, boolean overwrite) {
        this.artifact = artifact;
        this.log = log;
        this.rootPath = rootPath;
        this.createRepo = createRepo;
        this.overwrite = overwrite;
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
    public ArtifactSource generate(ProjectGenerator generator, ParameterValues arguments, String projectName) {
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
        log.info("  %s (%s in %s files)",
                Style.underline(FileUtils.relativize(resultFile)),
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
    public ModificationAttempt edit(ProjectEditor editor, ParameterValues arguments, String projectName) {
        return null;
    }

    @Override
    public ReviewResult review(ProjectReviewer reviewer, ParameterValues arguments, String projectName) {
        return null;
    }
}
