package com.atomist.rug.cli.command.repo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractRugScopedCommandInfo;

public class RepositoriesCommandInfo extends AbstractRugScopedCommandInfo {

    private static final List<String> commands = Arrays
            .asList(new String[] { "login", "configure" });

    public RepositoriesCommandInfo() {
        super(RepositoriesCommand.class, "repositories");
    }

    @Override
    public String description() {
        return "Login to GitHub to obtain a Personal Access Token";
    }

    @Override
    public String detail() {
        return "The Rug CLI uses your GitHub token to verify your membership in GitHub organizations "
                + "and Slack teams that have the Atomist Bot enrolled.  Those teams have acccess to "
                + "additional features, eg. team private Rug archives.";
    }

    @Override
    public Options options() {
        Options options = new Options();
        options.addOption(Option.builder().argName("USERNAME").desc("").longOpt("username")
                .optionalArg(false).hasArg(true).build());
        options.addOption(Option.builder().argName("MFA_CODE").desc("").longOpt("mfa-code")
                .optionalArg(false).hasArg(true).build());
        return options;
    }

    @Override
    public int order() {
        return -30;
    }

    @Override
    public String usage() {
        return "repositories SUBCOMMAND [OPTION]...";
    }

    @Override
    public List<String> subCommands() {
        return commands;
    }

    @Override
    public String group() {
        return "4";
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("repo");
    }
}
