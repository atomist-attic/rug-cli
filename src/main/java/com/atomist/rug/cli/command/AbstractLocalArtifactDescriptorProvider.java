package com.atomist.rug.cli.command;

import java.io.File;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.manifest.Manifest;
import com.atomist.rug.resolver.manifest.ManifestArtifactDescriptorCreator;
import com.atomist.rug.resolver.manifest.ManifestFactory;
import com.atomist.rug.resolver.manifest.MissingManifestException;
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
                "No manifest.yml or package.json found in .atomist folder. Please add a manifest.yml or package.json file and run the command again.",
                (String) null));
    }

    protected Optional<ArtifactDescriptor> localArtifactDescriptor(CommandLine commandLine) {
        String version = commandLine.getOptionValue('a');
        if (version == null) {
            version = "latest";
        }

        Optional<File> projectRoot = FileUtils.getWorkingDirectory();
        if (projectRoot.isPresent()) {
            ArtifactSource source = ArtifactSourceUtils
                    .createManifestOnlyArtifactSource(projectRoot.get());

            try {
                Manifest manifest = ManifestFactory.read(source);
                if (manifest != null) {
                    return Optional.of(new ManifestArtifactDescriptorCreator().create(manifest,
                            projectRoot.get().toURI()));
                }
            }
            catch (MissingManifestException e) {
                // We accept the fact that the manifest is missing or invalid
            }
        }
        return Optional.empty();
    }
}
