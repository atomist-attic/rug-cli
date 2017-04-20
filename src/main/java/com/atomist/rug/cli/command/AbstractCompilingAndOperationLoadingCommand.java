package com.atomist.rug.cli.command;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;

import com.atomist.project.archive.ArchiveRugResolver;
import com.atomist.project.archive.Coordinate;
import com.atomist.project.archive.Dependency;
import com.atomist.project.archive.ResolvedDependency;
import com.atomist.project.archive.RugResolver;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.cli.output.ConsoleLogger;
import com.atomist.rug.cli.output.ProgressReporterUtils;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.compiler.Compiler;
import com.atomist.rug.compiler.CompilerListener;
import com.atomist.rug.compiler.typescript.TypeScriptCompiler;
import com.atomist.rug.compiler.typescript.compilation.CompilerFactory;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.LocalArtifactDescriptor;
import com.atomist.source.ArtifactSource;

import scala.Option;
import scala.collection.JavaConverters;

public abstract class AbstractCompilingAndOperationLoadingCommand extends AbstractCommand {

    private static final String ENABLE_COMPILER_CACHE_KEY = "enable_compiler_cache";
    private static final boolean ENABLE_COMPILER_CACHE = true;

    protected Log log = new Log(getClass());

    @Override
    protected final void run(ArtifactDescriptor artifact, CommandLine commandLine) {
        if (artifact != null && artifact.extension() == Extension.ZIP
                && registry.findCommand(getClass()).loadArtifactSource()) {
            if (CommandContext.contains(ArtifactSource.class)
                    && CommandContext.contains(ResolvedDependency.class)) {
                ArtifactSource source = CommandContext.restore(ArtifactSource.class);
                RugResolver resolver = CommandContext.restore(RugResolver.class);

                ResolvedDependency rugs = new ProgressReportingOperationRunner<ResolvedDependency>(
                        String.format("Loading %s", ArtifactDescriptorUtils.coordinates(artifact)))
                                .run(indicator -> CommandContext.restore(ResolvedDependency.class));

                run(rugs, artifact, source, resolver, commandLine);
            }
            else {
                ArtifactSource source = ArtifactSourceUtils.createArtifactSource(artifact);
                CommandEventListenerRegistry
                        .raiseEvent((c) -> c.artifactSourceLoaded(artifact, source));

                ArtifactSource compiledSource = compile(artifact, source);
                CommandEventListenerRegistry
                        .raiseEvent((c) -> c.artifactSourceCompiled(artifact, compiledSource));

                RugResolver resolver = createRugResolver(artifact, compiledSource);

                ResolvedDependency rugs = resolver.resolvedDependencies();
                CommandEventListenerRegistry
                        .raiseEvent((c) -> c.operationsLoaded(artifact, rugs, resolver));

                run(rugs, artifact, compiledSource, resolver, commandLine);
            }
        }
        else {
            run(null, artifact, null, null, commandLine);
        }
    }

    private ArtifactSource compile(ArtifactDescriptor artifact, ArtifactSource source) {
        // Only compile local archives
        if (artifact instanceof LocalArtifactDescriptor) {

            // Get all registered and supported compilers
            Collection<Compiler> compilers = loadCompilers(artifact, source);

            if (!compilers.isEmpty()) {

                return new ProgressReportingOperationRunner<ArtifactSource>(
                        "Invoking compilers on project sources").run(indicator -> {
                            ArtifactSource compiledSource = source;
                            for (Compiler compiler : compilers) {
                                return compiler.compile(compiledSource);
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

        TypeScriptCompiler compiler;
        if (CommandContext.contains(TypeScriptCompiler.class)) {
            compiler = CommandContext.restore(TypeScriptCompiler.class);
        }
        else {
            if (SettingsReader.read().getConfigValue(ENABLE_COMPILER_CACHE_KEY,
                    ENABLE_COMPILER_CACHE)) {
                compiler = new TypeScriptCompiler(CompilerFactory
                        .cachingCompiler(CompilerFactory.create(), root.getAbsolutePath()));
            }
            else {
                compiler = new TypeScriptCompiler(CompilerFactory.create());
            }
            compiler.registerListener(new ReportingCompilerListener());
            CommandContext.save(TypeScriptCompiler.class, compiler);
        }

        if (compiler.supports(source)) {
            // Make sure the target dir for the compiler exists; eg. install command deletes it
            if (!root.exists()) {
                root.mkdirs();
            }
            return Collections.singletonList(compiler);
        }
        else {
            return Collections.emptyList();
        }
    }

    private RugResolver createRugResolver(ArtifactDescriptor artifact, ArtifactSource source) {
        return new ProgressReportingOperationRunner<RugResolver>(
                String.format("Loading %s", ArtifactDescriptorUtils.coordinates(artifact)))
                        .run(indicator -> {
                            Dependency root = processArtifact(artifact, source);
                            return new ArchiveRugResolver(root, ConsoleLogger.consoleLogger());
                        });
    }

    private Dependency processArtifact(ArtifactDescriptor node, ArtifactSource source) {
        List<Dependency> children = node.dependencies().stream()
                .filter(d -> d.extension() == Extension.ZIP)
                .map(d -> processArtifact(d, ArtifactSourceUtils.createArtifactSource(d)))
                .collect(Collectors.toList());
        return new Dependency(source,
                Option.apply(new Coordinate(node.group(), node.artifact(), node.version())),
                JavaConverters.asScalaBufferConverter(children).asScala());
    }

    protected abstract void run(ResolvedDependency rugs, ArtifactDescriptor artifact,
            ArtifactSource source, RugResolver resolver, CommandLine commandLine);

    private static class ReportingCompilerListener implements CompilerListener {

        private Log log = new Log(ReportingCompilerListener.class);

        @Override
        public void compileFailed(String path) {
            log.info("  Compiled " + path + " " + Style.red("failed"));
        }

        @Override
        public void compileStarted(String path) {
            if (CommandLineOptions.hasOption("V") && path != null) {
                int ix = path.lastIndexOf('/');
                ProgressReporterUtils.detail(path.substring(ix + 1));
            }
        }

        @Override
        public void compileSucceeded(String path, String content) {
            ProgressReporterUtils.detail(null);
            if (CommandLineOptions.hasOption("X") && content != null) {
                log.info("  Compiled " + Style.yellow(path) + " " + Style.green("succeeded"));
                if (!path.endsWith(".js.map")) {
                    log.info("  " + content.replace("\n", "\n  "));
                }
            }
            else if (CommandLineOptions.hasOption("V")) {
                log.info("  Compiled " + path + " " + Style.green("succeeded"));
            }
        }
    }

}
