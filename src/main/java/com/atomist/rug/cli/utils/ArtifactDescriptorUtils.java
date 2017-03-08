package com.atomist.rug.cli.utils;

import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.LocalArtifactDescriptor;

public abstract class ArtifactDescriptorUtils {

    public static String coordinates(ArtifactDescriptor artifact) {
        if (artifact instanceof LocalArtifactDescriptor) {
            return String.format("%s:%s [%s|local]", artifact.group(), artifact.artifact(),
                    artifact.version());
        }
        return String.format("%s:%s [%s|%s]", artifact.group(), artifact.artifact(),
                artifact.version(), artifact.extension().toString().toLowerCase());
    }
}
