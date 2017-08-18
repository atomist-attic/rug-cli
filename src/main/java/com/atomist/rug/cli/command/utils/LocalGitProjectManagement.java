package com.atomist.rug.cli.command.utils;

import com.atomist.graph.GraphNode;
import com.atomist.param.ParameterValues;
import com.atomist.project.archive.RugResolver;
import com.atomist.project.edit.*;
import com.atomist.project.generate.ProjectGenerator;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.tree.ArtifactSourceTreeCreator;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.kind.DefaultTypeRegistry$;
import com.atomist.rug.kind.core.ChangeLogEntry;
import com.atomist.rug.kind.core.ProjectContext;
import com.atomist.rug.kind.core.RepoResolver;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.project.ProvenanceInfoWriter;
import com.atomist.rug.runtime.js.RugContext;
import com.atomist.rug.runtime.js.interop.jsGitProjectLoader;
import com.atomist.rug.runtime.js.interop.jsPathExpressionEngine;
import com.atomist.rug.runtime.plans.ProjectManagement;
import com.atomist.rug.spi.Handlers;
import com.atomist.rug.spi.TypeRegistry;
import com.atomist.source.*;
import com.atomist.source.file.*;
import com.atomist.source.git.FileSystemGitArtifactSource;
import com.atomist.source.git.GitRepositoryCloner;
import com.atomist.tree.IdentityTreeMaterializer$;
import com.atomist.tree.TreeMaterializer;
import com.atomist.tree.pathexpression.PathExpressionEngine;
import difflib.DiffUtils;
import scala.Option;
import scala.collection.JavaConverters;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    private final RugResolver resolver;

    public LocalGitProjectManagement(ArtifactDescriptor artifact, String rootPath,
                                     boolean createRepo, boolean overwrite, boolean commit, boolean dryRun,
                                     RugResolver resolver) {
        this.artifact = artifact;
        this.rootPath = rootPath;
        this.createRepo = createRepo;
        this.overwrite = overwrite;
        this.commit = commit;
        this.dryRun = dryRun;
        this.resolver = resolver;
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
                .run(indicator -> generator.generate(projectName, arguments,
                        new ProjectContext(new LocalRugContext())));

        // Add provenance info to output
        result = new ProvenanceInfoWriter().write(result, generator, arguments,
                Constants.cliClient(), resolver);
        // Filter out any META-INF/maven/ files
        result = ArtifactSourceUtils.filterMetaInf(result);

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
        LogVisitor visitor = new LogVisitor();
        ArtifactSourceTreeCreator.visitTree(result, visitor);
        visitor.log(log);
        if (createRepo) {
            log.newline();
            GitUtils.initializeRepoAndCommitFiles(generator, arguments, root, resolver);
        }
        log.newline();
        log.info(Style.green("Successfully generated new project %s", projectName));

        return new FileSystemArtifactSource(fsid);
    }

    @Override
    public ModificationAttempt edit(ProjectEditor editor, ParameterValues arguments,
                                    String projectName, Option<Handlers.EditorTarget> target) {
        File root = FileUtils.createProjectRoot(projectName);

        if (commit) {
            GitUtils.isClean(root, "edit");
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
                    Constants.cliClient(), resolver);

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
                GitUtils.commitFiles(editor, arguments, root, resolver);
            }

            log.newline();
            log.info(Style.green("Successfully edited project %s", root.getName()));
        } else if (result instanceof NoModificationNeeded) {
            log.newline();
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Project"));
            log.info("  %s (%s in %s files)", Style.underline(FileUtils.relativize(root)),
                    FileUtils.sizeOf(root), source.allFiles().size());
            log.newline();
            log.info(Style.yellow("Editor made no changes to project %s", root.getName()));
        } else if (result instanceof FailedModificationAttempt) {
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
                } else if (!(delta.newFile() instanceof ByteArrayFileArtifact)) {
                    scala.Option<FileArtifact> opt = source.findFile(delta.newFile().path());
                    String existingContent = "";
                    if (opt.isDefined()) {
                        existingContent = opt.get().content();
                    }
                    String newContent = delta.newFile().content();
                    logPatch(delta.newFile().path(), delta.newFile().path(), existingContent,
                            newContent);
                }
            } else if (d instanceof FileUpdateDelta) {
                FileUpdateDelta delta = ((FileUpdateDelta) d);
                if (!dryRun) {
                    File file = new File(root, delta.oldFile().path());
                    file.delete();
                    writer.write(delta.updatedFile(), root);
                    logOperation("updated", delta.oldFile().path(), delta.updatedFile().path(),
                            root, delta.equals(lastDelta));
                } else if (!(delta.updatedFile() instanceof ByteArrayFileArtifact)) {
                    scala.Option<FileArtifact> opt = source.findFile(delta.updatedFile().path());
                    String existingContent = "";
                    if (opt.isDefined()) {
                        existingContent = opt.get().content();
                    }
                    String newContent = delta.updatedFile().content();
                    logPatch(delta.path(), delta.updatedFile().path(), existingContent, newContent);
                }
            } else if (d instanceof FileDeletionDelta) {
                FileDeletionDelta delta = (FileDeletionDelta) d;
                if (!dryRun) {
                    File file = new File(root, d.path());
                    file.delete();
                    logOperation("deleted", d.path(), null, root, delta.equals(lastDelta));
                } else if (!(delta.oldFile() instanceof ByteArrayFileArtifact)) {
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
        } else {
            sb.append("  ").append(Constants.TREE_NODE);
        }
        if (!oldPath.equals("") && !newPath.equals("") && !oldPath.equals(newPath)) {
            sb.append(Style.yellow(oldPath)).append(" ").append(Constants.DIVIDER).append(" ")
                    .append(Style.yellow(newPath)).append(" ").append(operation);
        } else if (!"".equals(newPath)) {
            sb.append(Style.yellow(newPath)).append(" ").append(operation);
        } else {
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
            } else if (diff.startsWith("-")) {
                log.info("  " + Style.red(diff));
            } else if (diff.startsWith("@@")) {
                log.info("  " + Style.cyan(diff));
            } else {
                log.info("  " + diff);
            }
        });
    }

    private static class LocalRugContext implements RugContext {

        @Override
        public Option<RepoResolver> repoResolver() {
            return Option.apply(new LocalRepoResolver());
        }

        @Override
        public TypeRegistry typeRegistry() {
            return DefaultTypeRegistry$.MODULE$;
        }

        @Override
        public GraphNode contextRoot() {
            return null;
        }

        @Override
        public jsPathExpressionEngine pathExpressionEngine() {
            return new jsPathExpressionEngine(this, typeRegistry(), new PathExpressionEngine());
        }

        @Override
        public String teamId() {
            return null;
        }

        @Override
        public TreeMaterializer treeMaterializer() {
            return IdentityTreeMaterializer$.MODULE$;
        }

        @Override
        public Object gitProjectLoader() {
            return new jsGitProjectLoader(repoResolver());
        }
    }

    private static class LocalRepoResolver implements RepoResolver {

        private static final Log log = new Log(LocalRepoResolver.class);

        private GitRepositoryCloner cloner;

        @Override
        public ArtifactSource resolveBranch(String owner, String repo, String branch) {
            log.info(String.format("Cloning %s/%s#%s", owner, repo, branch));
            init();
            try {
                File file = cloner.clone(repo, owner, Option.apply(branch), Option.empty(),
                        Option.empty(), 10);
                return new FileSystemGitArtifactSource(new NamedFileSystemArtifactSourceIdentifier(repo, file));
            } catch (ArtifactSourceException e) {
                throw new RuntimeException("Failed to resolve branch", e);
            }
        }

        @Override
        public ArtifactSource resolveSha(String owner, String repo, String sha) {
            log.info(String.format("Cloning %s/%s#%s", owner, repo, sha));
            init();
            try {
                File file = cloner.clone(repo, owner, Option.empty(), Option.apply(sha),
                        Option.empty(), 10);
                return new FileSystemGitArtifactSource(new NamedFileSystemArtifactSourceIdentifier(repo, file));
            } catch (ArtifactSourceException e) {
                throw new RuntimeException("Failed to resolve sha", e);
            }
        }

        @Override
        public String resolveBranch$default$3() {
            return "master";
        }

        private void init() {
            Optional<String> token = SettingsReader.read().getConfigValue(Settings.GIHUB_TOKEN_KEY,
                    String.class);
            String url = SettingsReader.read().getConfigValue("git_url", "https://github.com");
            if (!token.isPresent()) {
                throw new CommandException(
                        "No GitHub token configured. Please run the login command before running this generator.",
                        "generate");
            }
            cloner = new GitRepositoryCloner(token.get(), url);
        }
    }
}
