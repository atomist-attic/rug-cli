package com.atomist.rug.cli.command;

import com.atomist.rug.resolver.ArtifactDescriptor;

public interface Command {

    void run(String... args);

    void run(ArtifactDescriptor artifact, String... args);

}
