package com.atomist.rug.cli.command.extension;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.resolver.DependencyResolverFactory;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.version.VersionUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptorFactory;
import com.atomist.rug.resolver.DependencyResolver;

public class ExtensionCommand extends AbstractAnnotationBasedCommand {

    @Command
    public void run(CommandLine commandLine,
            @Argument(index = 1, defaultValue = "") String subcommand,
            @Argument(index = 2) String ext, @Option("a") String version) {

        switch (subcommand) {
        case "install":
            installExtension(createArtifactDescriptor(ext, version));
            break;
        case "uninstall":
            uninstallExtension(createArtifactDescriptor(ext, version));
            break;
        case "list":
            listExtension();
            break;
        default:
            throw new CommandException("Not enough or invalid arguments provided.", "extension");
        }
    }

    protected File getPathForExtension(ArtifactDescriptor artifact) {
        return new File(FileUtils.getUserDirectory(),
                ".atomist" + File.separator + "ext" + File.separator
                        + artifact.group().replace(".", File.separator) + File.separator
                        + artifact.artifact());
    }

    protected void printNoExtensionsInstalled() {
        log.info("  " + Style.yellow("No extensions installed"));
        log.newline();
    }

    private ArtifactDescriptor createArtifactDescriptor(String ext, String version) {
        if (ext != null && ext.contains(":")) {
            return ArtifactDescriptorFactory.create(ext, version);
        }
        else {
            throw new CommandException("No valid EXTENSION provided.", "extension");
        }
    }

    private File createPathForExtension(ArtifactDescriptor artifact) {
        File root = getPathForExtension(artifact);
        if (root.exists()) {
            FileUtils.deleteQuietly(root);
        }
        root.mkdirs();
        return root;
    }

    private void installExtension(ArtifactDescriptor artifact) {
        List<ArtifactDescriptor> dependencies = new ProgressReportingOperationRunner<List<ArtifactDescriptor>>(
                String.format("Resolving dependencies for extension %s",
                        ArtifactDescriptorUtils.coordinates(artifact))).run((indicator) -> {

                            DependencyResolver dependencyResolver = new DependencyResolverFactory()
                                    .createDependencyResolver(artifact, indicator);
                            // Resolve version
                            String newVersion = dependencyResolver.resolveVersion(artifact);
                            ArtifactDescriptor resolvedArtifact = ArtifactDescriptorFactory
                                    .copyFrom(artifact, newVersion);

                            // Read direct dependencies to make sure the cli is compatible
                            List<ArtifactDescriptor> directDependencies = dependencyResolver
                                    .resolveDirectDependencies(resolvedArtifact);
                            if (verifyVersionRange(resolvedArtifact, directDependencies)) {
                                return dependencyResolver.resolveTransitiveDependencies(
                                        ArtifactDescriptorFactory.copyFrom(artifact, newVersion));
                            }
                            else {
                                throw new CommandException(String.format(
                                        "Extension %s:%s:%s is not compatible.",
                                        resolvedArtifact.group(), resolvedArtifact.artifact(),
                                        resolvedArtifact.version()));
                            }
                        });

        File extensionRoot = createPathForExtension(artifact);

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Extension"));
        log.info("  %s", Style
                .underline(com.atomist.rug.cli.utils.FileUtils.relativize(extensionRoot.toURI())));

        ArtifactDescriptor lastArtifact = dependencies.stream().reduce((d1, d2) -> d2).orElse(null);
        dependencies.forEach(d -> installExtension(d, extensionRoot, lastArtifact.equals(d)));

        log.newline();
        log.info(Style.green("Successfully installed extension %s:%s", artifact.group(),
                artifact.artifact()));
    }

    private void installExtension(ArtifactDescriptor artifact, File root, boolean last) {
        try {
            File source = new File(artifact.uri());
            FileUtils.copyFile(source, new File(root, source.getName()), true);
            StringBuilder sb = new StringBuilder();
            if (last) {
                sb.append("  ").append(Constants.LAST_TREE_NODE);
            }
            else {
                sb.append("  ").append(Constants.TREE_NODE);
            }
            sb.append(Style.yellow(source.getName()));
            log.info(sb.toString());
        }
        catch (IOException e) {
        }
    }

    private void listExtension() {
        List<File> extensions = new ProgressReportingOperationRunner<List<File>>(
                "Listing extensions").run((indicator) -> {
                    File extensionRoot = new File(FileUtils.getUserDirectory(),
                            Constants.ATOMIST_ROOT + File.separator + "ext");

                    if (extensionRoot.exists()) {
                        return FileUtils.listFiles(extensionRoot, new String[] { "jar" }, true)
                                .stream()
                                .filter(e -> e.getName()
                                        .startsWith(e.getParentFile().getName() + "-"))
                                .collect(Collectors.toList());
                    }
                    return Collections.emptyList();
                });
        
        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Extensions"));

        if (extensions.size() > 0) {
            extensions.forEach(e -> {
                log.info("  %s", Style.underline(
                        com.atomist.rug.cli.utils.FileUtils.relativize(e.getParentFile().toURI())));
                StringBuilder sb = new StringBuilder();
                sb.append("  ").append(Constants.LAST_TREE_NODE);
                sb.append(Style.yellow(e.getName()));
                log.info(sb.toString());
            });
            log.newline();
        }
        else {
            printNoExtensionsInstalled();
        }
    }

    private void uninstallExtension(ArtifactDescriptor artifact) {
        File extensionRoot = getPathForExtension(artifact);
        if (!extensionRoot.exists()) {
            throw new CommandException(String.format("Extension %s:%s is not installed.",
                    artifact.group(), artifact.artifact()));
        }

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Extension"));
        log.info("  %s", Style
                .underline(com.atomist.rug.cli.utils.FileUtils.relativize(extensionRoot.toURI())));

        try {
            FileUtils.deleteDirectory(extensionRoot);
        }
        catch (IOException e) {
            throw new RunnerException("Error occurred uninstalling extension.", e);
        }

        log.newline();
        log.info(Style.green("Successfully uninstalled extension %s:%s", artifact.group(),
                artifact.artifact()));
    }

    private boolean verifyVersionRange(ArtifactDescriptor extension,
            List<ArtifactDescriptor> dependencies) {
        Optional<ArtifactDescriptor> cliDependency = dependencies.stream().filter(
                d -> d.group().equals(Constants.GROUP) && d.artifact().equals(Constants.ARTIFACT))
                .findFirst();

        if (cliDependency.isPresent()) {
            try {
                VersionScheme scheme = new GenericVersionScheme();
                VersionConstraint constraint = scheme
                        .parseVersionConstraint(cliDependency.get().version());
                Version cliVersion = scheme.parseVersion(VersionUtils.readVersion().get());
                if (constraint.containsVersion(cliVersion)) {
                    return true;
                }
                else {
                    log.info("Extension %s:%s:%s requires %s of %s:%s", extension.group(),
                            extension.artifact(), extension.version(), constraint.toString(),
                            Constants.GROUP, Constants.RUG_ARTIFACT);
                    return false;
                }
            }
            catch (InvalidVersionSpecificationException e) {
                // This will be captured earlier when resolving the dependencies.
            }
        }
        return true;
    }
}