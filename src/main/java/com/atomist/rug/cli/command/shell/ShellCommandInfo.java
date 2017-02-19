package com.atomist.rug.cli.command.shell;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractVersionCommandInfo;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;

public class ShellCommandInfo extends AbstractVersionCommandInfo {

    public ShellCommandInfo() {
        super(ShellCommand.class, "shell", 1);
    }

    @Override
    public String description() {
        return "Start a shell for the specified Rug archive";
    }

    @Override
    public String detail() {
        return "ARCHIVE should be a full name of an Rug archive, e.g., \"atomist:spring-service\".";
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE - 20;
    }

    @Override
    public String usage() {
        return "shell [OPTION]... ARCHIVE";
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList(new String[] { "sh", "repl", "load" });
    }

    @Override
    public String group() {
        return "5";
    }

    @Override
    public ArtifactDescriptor artifactDescriptor(CommandLine commandLine) {
        try {
            return super.artifactDescriptor(commandLine);
        }
        catch (CommandException e) {
            return new DefaultArtifactDescriptor(Constants.GROUP, Constants.RUG_ARTIFACT,
                    CommandUtils.readRugVersionFromPom(), Extension.JAR);
        }
    }

    @Override
    public boolean enabled(ArtifactDescriptor artifact) {
        return true;
    }
}
