package com.atomist.rug.cli.command;

import com.atomist.rug.resolver.ArtifactDescriptor;
import org.apache.commons.cli.CommandLine;

public interface ArtifactDescriptorProvider {

    ArtifactDescriptor artifactDescriptor(CommandLine commandLine);
}
