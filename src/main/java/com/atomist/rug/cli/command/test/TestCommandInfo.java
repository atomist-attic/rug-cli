package com.atomist.rug.cli.command.test;

import com.atomist.rug.cli.command.AbstractLocalArtifactDescriptorProvider;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.LocalArtifactDescriptor;

public class TestCommandInfo extends AbstractLocalArtifactDescriptorProvider {

    public TestCommandInfo() {
        super(TestCommand.class, "test");
    }

    @Override
    public String description() {
        return "Run test scenarios";
    }

    @Override
    public String detail() {
        return "TEST is the name of a test scenario.  If no TEST is specified, all scenarios will run.";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public String usage() {
        return "test [OPTION]... [TEST]";
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
