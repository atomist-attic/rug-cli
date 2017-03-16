package com.atomist.rug.cli.command.clean;

import com.atomist.rug.cli.command.AbstractLocalArtifactDescriptorProvider;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.LocalArtifactDescriptor;

public class CleanCommandInfo extends AbstractLocalArtifactDescriptorProvider {

    public CleanCommandInfo() {
        super(CleanCommand.class, "clean");
    }

    @Override
    public String description() {
        return "Clean up project";
    }

    @Override
    public String detail() {
        return "Clean up all temporarily created files and directories from the project.";
    }

    @Override
    public int order() {
        return 55;
    }

    @Override
    public String usage() {
        return "clean [OPTION]...";
    }

    @Override
    public String group() {
        return "2";
    }

    @Override
    public boolean enabled(ArtifactDescriptor artifact) {
        return artifact instanceof LocalArtifactDescriptor
                || artifact.extension().equals(Extension.ZIP);
    }
}
