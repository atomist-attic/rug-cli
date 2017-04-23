package com.atomist.rug.cli.command.login;

import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractRugScopedCommandInfo;

public class LoginCommandInfo extends AbstractRugScopedCommandInfo {

    public LoginCommandInfo() {
        super(LoginCommand.class, "login");
    }

    @Override
    public String description() {
        return "Login using GitHub authentication";
    }

    @Override
    public String detail() {
        return "The Rug CLI uses your GitHub token to verify your membership in GitHub organizations "
                + "and Slack teams that have the Atomist Bot enrolled.  Those teams have acccess to "
                + "additional features, eg. team private Rug archives.  Once you used the 'login' command, "
                + "you can run 'configure repositories' to configure access to your team's artifact repositories.";
    }

    @Override
    public Options options() {
        Options options = new Options();
        options.addOption(Option.builder().argName("USERNAME").desc("GitHub username")
                .longOpt("username").optionalArg(false).hasArg(true).build());
        options.addOption(Option.builder().argName("MFA_CODE")
                .desc("GitHub MFA code (only required if MFA is enabled)").longOpt("mfa-code")
                .optionalArg(false).hasArg(true).build());
        return options;
    }

    @Override
    public int order() {
        return -40;
    }

    @Override
    public String usage() {
        return "login [OPTION]...";
    }

    @Override
    public String group() {
        return "4";
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("lg");
    }
}
