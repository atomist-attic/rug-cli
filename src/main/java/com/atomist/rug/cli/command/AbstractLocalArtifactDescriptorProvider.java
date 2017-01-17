package com.atomist.rug.cli.command;

import java.io.File;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.manifest.Manifest;
import com.atomist.rug.manifest.ManifestArtifactDescriptorCreator;
import com.atomist.rug.manifest.ManifestFactory;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;

public abstract class AbstractLocalArtifactDescriptorProvider extends AbstractCommandInfo
        implements ArtifactDescriptorProvider {

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
        ArtifactSource source = ArtifactSourceUtils.createArtifactSource(projectRoot);

        Manifest manifest = ManifestFactory.read(source);
        if (manifest != null) {
            return new ManifestArtifactDescriptorCreator().create(manifest, projectRoot.toURI());
        }

        throw new CommandException("No manifest.yml found in .atomist folder. Please add a manifest.yml file and run the command again.",
                (String) null);
    }
}
