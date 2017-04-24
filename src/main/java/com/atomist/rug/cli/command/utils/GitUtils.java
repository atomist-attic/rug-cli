package com.atomist.rug.cli.command.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.atomist.param.ParameterValues;
import com.atomist.project.ProjectOperation;
import com.atomist.project.archive.RugResolver;
import com.atomist.project.generate.ProjectGenerator;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.resolver.project.ProvenanceInfoWriter;

public abstract class GitUtils {

    private static Log log = new Log(GitUtils.class);

    public static void initializeRepoAndCommitFiles(ProjectGenerator generator,
            ParameterValues arguments, File root, RugResolver resolver) {
        try (Git git = Git.init().setDirectory(root).call()) {
            log.info("Initialized a new git repository at " + git.getRepository().getDirectory());
            git.add().addFilepattern(".").call();
            RevCommit commit = git.commit().setAll(true).setMessage(String.format("%s\n\n```%s```",
                    StringUtils.capitalize(generator.description()), new ProvenanceInfoWriter()
                            .write(generator, arguments, Constants.cliClient(), resolver)))
                    .setAuthor("Atomist", "cli@atomist.com").call();
            log.info("Committed initial set of files to git repository (%s)",
                    commit.abbreviate(7).name());
        }
        catch (IllegalStateException | GitAPIException e) {
            throw new RunnerException(e);
        }
    }

    public static void commitFiles(ProjectOperation operation, ParameterValues arguments, File root,
            RugResolver resolver) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (Repository repository = builder.setGitDir(new File(root, ".git")).readEnvironment()
                .findGitDir().build()) {
            try (Git git = new Git(repository)) {
                log.info("Committing to git repository at " + git.getRepository().getDirectory());
                git.add().addFilepattern(".").call();
                RevCommit commit = git.commit().setAll(true)
                        .setMessage(String.format("%s\n\n```\n%s```",
                                StringUtils.capitalize(operation.description()),
                                new ProvenanceInfoWriter().write(operation, arguments,
                                        Constants.cliClient(), resolver)))
                        .setAuthor("Atomist", "cli@atomist.com").call();
                log.info("Committed changes to git repository (%s)", commit.abbreviate(7).name());
            }
        }
        catch (IllegalStateException | IOException | GitAPIException e) {
            throw new RunnerException(e);
        }
    }

    public static void isClean(File root, String command) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        builder.findGitDir(root).readEnvironment();
        if (builder.getGitDir() == null) {
            builder.setGitDir(root);
        }
        try (Repository repository = builder.build()) {
            try (Git git = new Git(repository)) {
                Status status = git.status().call();
                if (!status.isClean()) {
                    throw new CommandException(String.format(
                            "Working tree at %s not clean.\nPlease commit or stash your changes before running this command.",
                            root.getAbsolutePath()), command);
                }
            }
        }
        catch (NoWorkTreeException e) {
            // do nothing
        }
        catch (IllegalStateException | IOException | GitAPIException e) {
            throw new RunnerException(e);
        }
    }
}
