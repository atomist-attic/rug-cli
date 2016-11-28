package com.atomist.rug.cli.utils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.LocalArtifactDescriptor;

public abstract class ArtifactDescriptorUtils {

    public static String coordinates(ArtifactDescriptor artifact) {
        if (artifact instanceof LocalArtifactDescriptor) {
            return String.format("%s:%s:%s %s local", artifact.group(), artifact.artifact(),
                    artifact.version(), Constants.REDIVID);
        }
        return String.format("%s:%s:%s", artifact.group(), artifact.artifact(), artifact.version());
    }
}
