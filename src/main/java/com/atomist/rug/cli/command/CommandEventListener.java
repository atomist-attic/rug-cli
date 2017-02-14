package com.atomist.rug.cli.command;

import com.atomist.project.archive.Rugs;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;

public interface CommandEventListener {
    
    void started(CommandInfo info, ArtifactDescriptor artifact);

    void artifactSourceLoaded(ArtifactDescriptor artifact, ArtifactSource source);
    
    void artifactSourceCompiled(ArtifactDescriptor artifact, ArtifactSource source);
    
    void operationsLoaded(ArtifactDescriptor artifact, Rugs operations);
    
    void finished(CommandInfo info, ArtifactDescriptor artifact);
}
