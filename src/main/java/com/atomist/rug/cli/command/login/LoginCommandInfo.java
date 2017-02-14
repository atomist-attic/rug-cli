package com.atomist.rug.cli.command.login;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractRugScopedCommandInfo;

public class LoginCommandInfo extends AbstractRugScopedCommandInfo {

    public LoginCommandInfo() {
        super(LoginCommand.class, "login");
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
        return "login [OPTION]...";
    }
    
    @Override
    public String group() {
        return "admin";
    }
}
