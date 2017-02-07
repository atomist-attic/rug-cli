package com.atomist.rug.cli.command;

import java.io.File;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.manifest.Manifest;
import com.atomist.rug.manifest.ManifestArtifactDescriptorCreator;
import com.atomist.rug.manifest.ManifestFactory;
import com.atomist.rug.manifest.MissingManifestException;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;

public abstract class AbstractLocalArtifactDescriptorProvider extends AbstractCommandInfo
        implements ArtifactDescriptorProvider {

    public AbstractLocalArtifactDescriptorProvider(Class<? extends Command> commandClass,
            String commandName) {
        super(commandClass, commandName);
    }

    public ArtifactDescriptor artifactDescriptor(CommandLine commandLine) {
        Optional<ArtifactDescriptor> artifact = localArtifactDescriptor(commandLine);

        return artifact.orElseThrow(() -> new CommandException(
                "No manifest.yml found in .atomist folder. Please add a manifest.yml file and run the command again.",
                (String) null));
    }

    protected Optional<ArtifactDescriptor> localArtifactDescriptor(CommandLine commandLine) {
        String version = commandLine.getOptionValue('a');
        if (version == null) {
            version = "latest";
        }

        File projectRoot = CommandUtils.getRequiredWorkingDirectory();
        ArtifactSource source = ArtifactSourceUtils.createManifestOnlyArtifactSource(projectRoot);
        
        try {
            Manifest manifest = ManifestFactory.read(source);
            if (manifest != null) {
                return Optional.of(new ManifestArtifactDescriptorCreator().create(manifest,
                        projectRoot.toURI()));
            }
        }
        catch (MissingManifestException e) {
            // We accept the fact that the manifest is missing or invalid
        }
        return Optional.empty();
    }
}
