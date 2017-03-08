package com.atomist.rug.cli.utils;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.LocalArtifactDescriptor;

public abstract class ArtifactDescriptorUtils {

    public static String coordinates(ArtifactDescriptor artifact) {
        String format = "%s:%s " + "(%s" + Constants.DOT + "%s)";
        if (artifact instanceof LocalArtifactDescriptor) {
            return String.format(format, artifact.group(), artifact.artifact(), artifact.version(),
                    "local");
        }
        return String.format(format, artifact.group(), artifact.artifact(), artifact.version(),
                artifact.extension().toString().toLowerCase());
    }
}
