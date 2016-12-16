package com.atomist.rug.cli.command;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.manifest.Manifest;
import com.atomist.rug.manifest.ManifestArtifactDescriptorCreator;
import com.atomist.rug.manifest.ManifestFactory;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;

public abstract class AbstractLocalArtifactDescriptorProvider extends AbstractCommandInfo
        implements ArtifactDescriptorProvider, ClasspathEntryProvider {

    public AbstractLocalArtifactDescriptorProvider(Class<? extends Command> commandClass,
            String commandName) {
        super(commandClass, commandName);
    }

    public ArtifactDescriptor artifactDescriptor(CommandLine commandLine) {
        String version = commandLine.getOptionValue('a');
        if (version == null) {
            version = "latest";
        }

        File projectRoot = CommandUtils.getRequiredWorkingDirectory();
        ArtifactSource source = new FileSystemArtifactSource(
                new SimpleFileSystemArtifactSourceIdentifier(projectRoot));

        Manifest manifest = ManifestFactory.read(source);
        if (manifest != null) {
            return new ManifestArtifactDescriptorCreator().create(manifest, projectRoot.toURI());
        }

        throw new CommandException("No manifest.yml or package.json found in .atomist folder",
                (String) null);
    }

    @Override
    public List<URL> classpathEntries(ArtifactDescriptor artifact) {
        Options options = options();
        if (options.hasOption("l") && CommandLineOptions.hasOption("l")) {
            return addNodeModulesToClasspath();
        }
        else if (!options.hasOption("l")) {
            return addNodeModulesToClasspath();
        }
        return Collections.emptyList();
    }

    private List<URL> addNodeModulesToClasspath() {
        File currentWorkingRoot = CommandUtils.getRequiredWorkingDirectory();
        File nodeModuleRoot = new File(CommandUtils.getRequiredWorkingDirectory(), ".atomist/node_modules");
        List<URL> urls = new ArrayList<>();
        if (nodeModuleRoot.exists()) {
            try {
                urls.add(nodeModuleRoot.toURI().toURL());
            }
            catch (MalformedURLException e) {
                // Can't happen
            }
        }
        if (currentWorkingRoot.exists()) {
            try {
                urls.add(currentWorkingRoot.toURI().toURL());
            }
            catch (MalformedURLException e) {
                // Can't happen
            }
        }
        return urls;
    }
}
