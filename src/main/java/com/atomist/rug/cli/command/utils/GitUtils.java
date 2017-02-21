package com.atomist.rug.cli.command.utils;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.atomist.project.ProjectOperation;
import com.atomist.project.ProjectOperationArguments;
import com.atomist.project.ProvenanceInfoWriter;
import com.atomist.project.generate.ProjectGenerator;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.command.CommandException;

public abstract class GitUtils {

    private static Log log = new Log(GitUtils.class);

    public static void initializeRepoAndCommitFiles(ProjectGenerator generator,
            ProjectOperationArguments arguments, File root) {
        try (Git git = Git.init().setDirectory(root).call()) {
            log.info("Initialized a new git repository at " + git.getRepository().getDirectory());
            git.add().addFilepattern(".").call();
            RevCommit commit = git.commit().setAll(true)
                    .setMessage(
                            String.format("%s\n\n```%s```", generator.description(),
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

    public static void commitFiles(ProjectOperation operation, ProjectOperationArguments arguments,
            File root) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File(root, ".git")).readEnvironment()
                .findGitDir().build()) {
            try (Git git = new Git(repository)) {
                log.info("Committing to git repository at " + git.getRepository().getDirectory());
                git.add().addFilepattern(".").call();
                RevCommit commit = git.commit().setAll(true)
                        .setMessage(String.format("%s\n\n```\n%s```", operation.description(),
                                new ProvenanceInfoWriter().write(operation, arguments,
                                        Constants.cliClient())))
                        .setAuthor("Atomist", "cli@atomist.com").call();
                log.info("Committed changes to git repository (%s)", commit.abbreviate(7).name());
            }
        }
        catch (IllegalStateException | IOException | GitAPIException e) {
            throw new RunnerException(e);
        }
    }

    public static void isClean(File root) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File(root, ".git")).readEnvironment()
                .findGitDir().build()) {
            try (Git git = new Git(repository)) {
                Status status = git.status().call();
                if (!status.isClean()) {
                    throw new CommandException(String.format(
                            "Working tree at %s not clean.\nPlease commit or stash your changes before running an editor with -R.",
                            root.getAbsolutePath()), "edit");
                }
            }
        }
        catch (IllegalStateException | IOException | GitAPIException e) {
            throw new RunnerException(e);
        }
    }
}
