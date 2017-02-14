package com.atomist.rug.cli.command;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.ArtifactDescriptorFactory;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;
import org.apache.commons.cli.CommandLine;

import java.util.Optional;

public abstract class AbstractRugScopedCommandInfo extends AbstractLocalArtifactDescriptorProvider
        implements ArtifactDescriptorProvider {

    public AbstractRugScopedCommandInfo(Class<? extends Command> commandClass, String commandName) {
        super(commandClass, commandName);
    }

    @Override
    public ArtifactDescriptor artifactDescriptor(CommandLine commandLine) {
        Optional<ArtifactDescriptor> artifact = localArtifactDescriptor(commandLine);
        if (artifact.isPresent()) {
            // We are not interested in the operations right now; we only need the classpath
            ArtifactDescriptor ad = artifact.get();
            return ArtifactDescriptorFactory.copyFrom(ad, ad.group(), ad.artifact(), ad.version(),
                    Extension.JAR);
        }
        else {
            return new DefaultArtifactDescriptor(Constants.GROUP, Constants.RUG_ARTIFACT,
                    CommandUtils.readRugVersionFromPom(), Extension.JAR);
        }
    }

    @Override
    public boolean loadArtifactSource() {
        return false;
    }
}
