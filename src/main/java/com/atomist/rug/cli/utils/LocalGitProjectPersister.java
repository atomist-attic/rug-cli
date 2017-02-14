package com.atomist.rug.cli.utils;

import com.atomist.param.ParameterValues;
import com.atomist.project.generate.ProjectGenerator;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.utils.GitUtils;
import com.atomist.rug.resolver.project.ProvenanceInfoWriter;
import com.atomist.rug.runtime.plans.ProjectPersister;
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
public class LocalGitProjectPersister implements ProjectPersister<FileSystemArtifactSource> {

    private final String rootName;
    private final boolean createRepo;
    private final boolean overwrite;

    public LocalGitProjectPersister(String rootName, boolean createRepo, boolean overwrite) {
        this.rootName = rootName;
        this.createRepo = createRepo;
        this.overwrite = overwrite;
    }

    /**
     * Persist the artifact source to disk, init as git repo if required, and return new ArtifactSource
     *
     * @param projectName
     * @param result
     * @return
     */
    @Override
    public FileSystemArtifactSource persist(ProjectGenerator generator, ParameterValues arguments, String projectName, ArtifactSource result) {

        final File root = createProjectRoot(rootName, projectName, overwrite);

        // Add provenance info to output
        result = new ProvenanceInfoWriter().write(result, generator, arguments,
                Constants.cliClient());

        FileSystemArtifactSourceIdentifier fsid = new SimpleFileSystemArtifactSourceIdentifier(root);

        new FileSystemArtifactSourceWriter().write(result,
                fsid,
                new SimpleSourceUpdateInfo(generator.name()));

        if (createRepo) {
            GitUtils.initializeRepoAndCommitFiles(generator, arguments, root);
        }

        return new FileSystemArtifactSource(fsid);
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
}
