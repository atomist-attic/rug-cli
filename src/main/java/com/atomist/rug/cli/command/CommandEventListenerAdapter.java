package com.atomist.rug.cli.command;

import com.atomist.rug.loader.OperationsAndHandlers;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;

public class CommandEventListenerAdapter implements CommandEventListener {
    
    @Override
    public void started(CommandInfo info, ArtifactDescriptor artifact) {
    }
    
    @Override
    public void artifactSourceLoaded(ArtifactDescriptor artifact, ArtifactSource source) {
    }
    
    @Override
    public void artifactSourceCompiled(ArtifactDescriptor artifact, ArtifactSource source) {
    }
    
    @Override
    public void operationsLoaded(ArtifactDescriptor artifact, OperationsAndHandlers operations) {
    }
    
    @Override
    public void finished(CommandInfo info, ArtifactDescriptor artifact) {
    }
}
