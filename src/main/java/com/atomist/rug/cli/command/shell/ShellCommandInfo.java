package com.atomist.rug.cli.command.shell;

import com.atomist.rug.cli.command.AbstractVersionCommandInfo;

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

}
