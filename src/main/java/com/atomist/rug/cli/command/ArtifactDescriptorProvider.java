package com.atomist.rug.cli.command;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.resolver.ArtifactDescriptor;

public interface ArtifactDescriptorProvider {

    ArtifactDescriptor artifactDescriptor(CommandLine commandLine);
}
