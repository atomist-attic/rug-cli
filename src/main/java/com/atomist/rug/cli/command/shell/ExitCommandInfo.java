package com.atomist.rug.cli.command.shell;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractRugScopedCommandInfo;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;

public class ExitCommandInfo extends AbstractRugScopedCommandInfo {

    public ExitCommandInfo() {
        super(ExitCommand.class, "exit");
    }

    @Override
    public String description() {
        return "Exit a shell session";
    }

    @Override
    public String detail() {
        return "";
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE - 5;
    }

    @Override
    public String usage() {
        return "exit [OPTION]...";
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList(new String[] { "quit", "q" });
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
        return Constants.isShell();
    }
}
