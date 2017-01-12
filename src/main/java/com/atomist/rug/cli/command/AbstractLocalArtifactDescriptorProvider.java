package com.atomist.rug.cli.command;

import java.io.File;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.manifest.Manifest;
import com.atomist.rug.manifest.ManifestArtifactDescriptorCreator;
import com.atomist.rug.manifest.ManifestFactory;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;

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
        ArtifactSource source = new FileSystemArtifactSource(
                new SimpleFileSystemArtifactSourceIdentifier(projectRoot));

//        ArtifactSource source = FileSystemArtifactSource$.MODULE$.apply(
//                new SimpleFileSystemArtifactSourceIdentifier(projectRoot),
//                new DotGitDirFilter(projectRoot.getPath()));

        Manifest manifest = ManifestFactory.read(source);
        if (manifest != null) {
            return new ManifestArtifactDescriptorCreator().create(manifest, projectRoot.toURI());
        }

        throw new CommandException("No manifest.yml or package.json found in .atomist folder",
                (String) null);
    }
}
