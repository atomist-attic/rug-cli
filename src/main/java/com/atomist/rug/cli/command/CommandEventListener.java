package com.atomist.rug.cli.command;

import com.atomist.project.archive.ResolvedDependency;
import com.atomist.project.archive.RugResolver;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.source.ArtifactSource;

public interface CommandEventListener {

    void started(CommandInfo info, ArtifactDescriptor artifact);

    void artifactSourceLoaded(ArtifactDescriptor artifact, ArtifactSource source);

    void artifactSourceCompiled(ArtifactDescriptor artifact, ArtifactSource source);

    void operationsLoaded(ArtifactDescriptor artifact, ResolvedDependency operations,
            RugResolver resolver);

    void finished(CommandInfo info, ArtifactDescriptor artifact);
}
