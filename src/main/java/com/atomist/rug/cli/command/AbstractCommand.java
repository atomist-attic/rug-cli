package com.atomist.rug.cli.command;

import static scala.collection.JavaConversions.asJavaCollection;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.atomist.event.SystemEvent;
import com.atomist.plan.TreeMaterializer;
import com.atomist.project.archive.Operations;
import com.atomist.rug.BadRugException;
import com.atomist.rug.RugRuntimeException;
import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.cli.command.utils.ParseExceptionProcessor;
import com.atomist.rug.cli.output.ConsoleUtils;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.settings.SettingsReader;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.compiler.Compiler;
import com.atomist.rug.compiler.ServiceLoaderCompilerRegistry;
import com.atomist.rug.kind.service.ConsoleMessageBuilder;
import com.atomist.rug.loader.DecoratingOperationsLoader;
import com.atomist.rug.loader.HandlerOperationsLoader;
import com.atomist.rug.loader.Handlers;
import com.atomist.rug.loader.OperationsAndHandlers;
import com.atomist.rug.loader.OperationsLoaderException;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.ArtifactDescriptor.Scope;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;
import com.atomist.rug.resolver.LocalArtifactDescriptor;
import com.atomist.rug.resolver.UriBasedDependencyResolver;
import com.atomist.source.ArtifactSource;
import com.atomist.source.Deltas;
import com.atomist.tree.TreeNode;
import com.atomist.tree.pathexpression.PathExpression;

public abstract class AbstractCommand implements com.atomist.rug.cli.command.Command {

    private CommandInfoRegistry registry = new ServiceLoadingCommandInfoRegistry();

    public void run(String... args) {

        ConsoleUtils.configureStreams();
        CommandLine commandLine = parseCommandLine(args);

        loadOperationsAndInvokeRun(null, null, commandLine);
    }

    @Override
    public final void run(String group, String artifactId, String version, boolean local, URI[] uri,
            String... args) {

        ConsoleUtils.configureStreams();
        CommandLine commandLine = parseCommandLine(args);

        ArtifactDescriptor artifact = createArtifactDescriptor(group, artifactId, version, local);
        loadOperationsAndInvokeRun(uri, artifact, commandLine);
    }

    private ArtifactDescriptor createArtifactDescriptor(String group, String artifactId,
            String version, boolean local) {
        ArtifactDescriptor artifact = null;
        if (local) {
            artifact = new LocalArtifactDescriptor(group, artifactId, version, Extension.ZIP,
                    Scope.COMPILE, CommandUtils.getRequiredWorkingDirectory().toURI());
        }
        else {
            File archive = new File(new SettingsReader().read().getLocalRepository().path(),
                    group.replace(".", File.separator) + File.separator + artifactId
                            + File.separator + version + File.separator + artifactId + "-" + version
                            + ".zip");
            artifact = new DefaultArtifactDescriptor(group, artifactId, version, Extension.ZIP,
                    Scope.COMPILE, archive.toURI());
        }
        return artifact;
    }

    private HandlerOperationsLoader createOperationsLoader(URI[] uri) {
        HandlerOperationsLoader loader = new DecoratingOperationsLoader(
                new UriBasedDependencyResolver(uri,
                        new SettingsReader().read().getLocalRepository().path())) {
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
                        public TreeNode rootNodeFor(SystemEvent arg0, PathExpression arg1) {
                            return null;
                        }
                    });

            return new OperationsAndHandlers(operations, handlers);
        }
        catch (Exception e) {
            if (e instanceof BadRugException) {
                throw new CommandException("Failed to load archive: \n" + e.getMessage(), e);
            }
            else if (e instanceof OperationsLoaderException) {
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
        if (artifact == null) {
            return null;
        }

        return new ProgressReportingOperationRunner<OperationsAndHandlers>(String
                .format("Loading %s into runtime", ArtifactDescriptorUtils.coordinates(artifact)))
                        .run(indicator -> {
                            return doLoadOperationsAndHandlers(artifact, source, loader);
                        });
    }

    private void loadOperationsAndInvokeRun(URI[] uri, ArtifactDescriptor artifact,
            CommandLine commandLine) {
        OperationsAndHandlers operationsAndHandlers = null;
        if (artifact != null) {

            ArtifactSource source = ArtifactSourceUtils.createArtifactSource(artifact);
            source = compile(source);
            operationsAndHandlers = loadOperationsAndHandlers(artifact, source,
                    createOperationsLoader(uri));
        }
        run(operationsAndHandlers, artifact, commandLine);
    }

    private CommandLine parseCommandLine(String... args) {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        try {

            Optional<CommandInfo> commandInfo = registry.commands().stream()
                    .filter(c -> c.className().equals(getClass().getName())).findFirst();
            if (commandInfo.isPresent()) {
                Options options = new Options();
                commandInfo.get().options().getOptions().stream().forEach(options::addOption);
                commandInfo.get().globalOptions().getOptions().stream().forEach(options::addOption);
                commandLine = parser.parse(options, args);
                CommandLineOptions.set(commandLine);
            }
            return commandLine;
        }
        catch (ParseException e) {
            throw new CommandException(ParseExceptionProcessor.process(e), (String) null);
        }
    }

    private ArtifactSource compile(ArtifactSource source) {
        // Get all registered and supported compilers
        Collection<Compiler> compilers = asJavaCollection(
                ServiceLoaderCompilerRegistry.findAll(source));

        if (!compilers.isEmpty()) {

            return new ProgressReportingOperationRunner<ArtifactSource>("Compiling script sources")
                    .run(indicator -> {
                        ArtifactSource compiledSource = source;
                        for (Compiler compiler : compilers) {
                            indicator.report(String.format(
                                    "Invoking %s compiler to compile %s script sources",
                                    compiler.name(), compiler.extension()));
                            ArtifactSource cs = compiler.compile(compiledSource);
                            Deltas deltas = cs.deltaFrom(compiledSource);
                            asJavaCollection(deltas.deltas())
                                    .forEach(d -> indicator.report("  Compiled " + d.path()));
                            compiledSource = cs;
                        }
                        return compiledSource;
                    });
        }
        else {
            return source;
        }
    }

    protected abstract void run(OperationsAndHandlers operationsAndHandlers,
            ArtifactDescriptor artifact, CommandLine commandLine);
}
