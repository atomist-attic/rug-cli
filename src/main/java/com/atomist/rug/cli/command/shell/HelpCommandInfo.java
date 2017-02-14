package com.atomist.rug.cli.command.shell;

import com.atomist.rug.cli.command.AbstractCommandInfo;

public class HelpCommandInfo extends AbstractCommandInfo {

    public HelpCommandInfo() {
        super(HelpCommand.class, "help");
    }

    @Override
    public String description() {
        return "Print usage";
    }

    @Override
    public String detail() {
        return "";
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE - 10;
    }

    @Override
    public String usage() {
        return "exit";
    }
    
    @Override
    public String group() {
        return "admin";
    }

}
