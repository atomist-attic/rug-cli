package com.atomist.rug.cli.command;

import static scala.collection.JavaConversions.asJavaCollection;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.springframework.util.StringUtils;

import com.atomist.event.SystemEvent;
import com.atomist.plan.TreeMaterializer;
import com.atomist.project.archive.Operations;
import com.atomist.rug.BadRugException;
import com.atomist.rug.RugRuntimeException;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.compiler.Compiler;
import com.atomist.rug.compiler.typescript.TypeScriptCompiler;
import com.atomist.rug.compiler.typescript.compilation.CompilerFactory;
import com.atomist.rug.kind.service.ConsoleMessageBuilder;
import com.atomist.rug.loader.DecoratingOperationsLoader;
import com.atomist.rug.loader.HandlerOperationsLoader;
import com.atomist.rug.loader.Handlers;
import com.atomist.rug.loader.OperationsAndHandlers;
import com.atomist.rug.loader.OperationsLoaderException;
import com.atomist.rug.loader.OperationsLoaderRuntimeException;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.LocalArtifactDescriptor;
import com.atomist.rug.resolver.UriBasedDependencyResolver;
import com.atomist.source.ArtifactSource;
import com.atomist.source.Deltas;
import com.atomist.tree.TreeNode;
import com.atomist.tree.pathexpression.PathExpression;

public abstract class AbstractCompilingAndOperationLoadingCommand extends AbstractCommand {

    protected Log log = new Log(getClass());

    @Override
    protected final void run(URI[] uri, ArtifactDescriptor artifact, CommandLine commandLine) {
        if (artifact != null && artifact.extension() == Extension.ZIP
                && registry.findCommand(commandLine).loadArtifactSource()) {

            if (CommandContext.contains(ArtifactSource.class)
                    && CommandContext.contains(OperationsAndHandlers.class)) {
                ArtifactSource source = CommandContext.restore(ArtifactSource.class);

                OperationsAndHandlers operationsAndHandlers = new ProgressReportingOperationRunner<OperationsAndHandlers>(
                        String.format("Loading %s",
                                ArtifactDescriptorUtils.coordinates(artifact))).run(indicator -> {
                                    return CommandContext.restore(OperationsAndHandlers.class);
                                });

                run(operationsAndHandlers, artifact, source, commandLine);
            }
            else {
                ArtifactSource source = ArtifactSourceUtils.createArtifactSource(artifact);
                CommandEventListenerRegistry
                        .raiseEvent((c) -> c.artifactSourceLoaded(artifact, source));

                ArtifactSource compiledSource = compile(artifact, source);
                CommandEventListenerRegistry
                        .raiseEvent((c) -> c.artifactSourceCompiled(artifact, compiledSource));

                OperationsAndHandlers operationsAndHandlers = loadOperationsAndHandlers(artifact,
                        compiledSource, createOperationsLoader(uri));
                CommandEventListenerRegistry
                        .raiseEvent((c) -> c.operationsLoaded(artifact, operationsAndHandlers));

                run(operationsAndHandlers, artifact, compiledSource, commandLine);
            }
        }
        else {
            run(null, artifact, null, commandLine);
        }
    }

    private ArtifactSource compile(ArtifactDescriptor artifact, ArtifactSource source) {
        // Only compile local archives
        if (artifact instanceof LocalArtifactDescriptor) {

            // Get all registered and supported compilers
            Collection<Compiler> compilers = loadCompilers(artifact, source);

            if (!compilers.isEmpty()) {

                return new ProgressReportingOperationRunner<ArtifactSource>(
                        "Processing script sources").run(indicator -> {
                            ArtifactSource compiledSource = source;
                            for (Compiler compiler : compilers) {
                                indicator.report(String.format("Invoking %s on %s script sources",
                                        compiler.name(),
                                        StringUtils.collectionToCommaDelimitedString(
                                                compiler.extensions())));
                                ArtifactSource cs = compiler.compile(compiledSource);
                                Deltas deltas = cs.deltaFrom(compiledSource);
                                if (deltas.empty()) {
                                    indicator.report("  No files modified");
                                }
                                else {
                                    asJavaCollection(deltas.deltas()).forEach(
                                            d -> indicator.report("  Created " + d.path()));
                                }
                                compiledSource = cs;
                            }
                            return compiledSource;
                        });
            }
        }
        return source;
    }

    private Collection<Compiler> loadCompilers(ArtifactDescriptor artifact, ArtifactSource source) {
        File root = new File(new File(artifact.uri()),
                ".atomist" + File.separator + "target" + File.separator + ".jscache");

        TypeScriptCompiler compiler = null;
        if (CommandContext.contains(TypeScriptCompiler.class)) {
            compiler = CommandContext.restore(TypeScriptCompiler.class);
        }
        else {
            compiler = new TypeScriptCompiler(CompilerFactory
                    .cachingCompiler(CompilerFactory.create(), root.getAbsolutePath()));
            CommandContext.save(TypeScriptCompiler.class, compiler);
        }

        // Make sure the target dir for the compiler exists; eg. install command deletes it
        if (!root.exists()) {
            root.mkdirs();
        }

        if (compiler.supports(source)) {
            return Collections.singletonList(compiler);
        }
        else {
            return Collections.emptyList();
        }
    }

    private HandlerOperationsLoader createOperationsLoader(URI[] uri) {
        HandlerOperationsLoader loader = new DecoratingOperationsLoader(
                new UriBasedDependencyResolver(uri,
                        SettingsReader.read().getLocalRepository().path())) {
            @Override
            protected List<ArtifactDescriptor> postProcessArfifactDescriptors(
                    ArtifactDescriptor artifact, List<ArtifactDescriptor> dependencies) {
                if (artifact instanceof LocalArtifactDescriptor) {
                    dependencies.add(artifact);
                }
                return dependencies;
            }
        };
        return loader;
    }

    private OperationsAndHandlers doLoadOperationsAndHandlers(ArtifactDescriptor artifact,
            ArtifactSource source, HandlerOperationsLoader loader) throws Exception {
        try {
            Operations operations = loader.load(artifact, source);
            Handlers handlers = loader.loadHandlers("", artifact, source,
                    new ConsoleMessageBuilder("", null), new TreeMaterializer() {
                        @Override
                        public TreeNode rootNodeFor(SystemEvent systemEvent,
                                PathExpression pathExpression) {
                            return null;
                        }

                        @Override
                        public TreeNode hydrate(String teamId, TreeNode treeNode,
                                PathExpression pathExpression) {
                            return null;
                        }
                    });

            return new OperationsAndHandlers(operations, handlers);
        }
        catch (Exception e) {
            if (e instanceof BadRugException) {
                throw new CommandException("Failed to load archive: \n" + e.getMessage(), e);
            }
            else if (e instanceof OperationsLoaderException
                    || e instanceof OperationsLoaderRuntimeException) {
                if (e.getCause() instanceof BadRugException) {
                    throw new CommandException(
                            "Failed to load archive: \n" + e.getCause().getMessage(), e);
                }
                else if (e.getCause() instanceof RugRuntimeException) {
                    throw new CommandException(
                            "Failed to load archive: \n" + e.getCause().getMessage(), e);
                }
            }
            throw e;
        }
    }

    private OperationsAndHandlers loadOperationsAndHandlers(ArtifactDescriptor artifact,
            ArtifactSource source, HandlerOperationsLoader loader) {
        if (artifact == null || source == null) {
            return null;
        }

        return new ProgressReportingOperationRunner<OperationsAndHandlers>(String
                .format("Loading %s", ArtifactDescriptorUtils.coordinates(artifact)))
                        .run(indicator -> {
                            return doLoadOperationsAndHandlers(artifact, source, loader);
                        });
    }

    protected abstract void run(OperationsAndHandlers operationsAndHandlers,
            ArtifactDescriptor artifact, ArtifactSource source, CommandLine commandLine);

}
