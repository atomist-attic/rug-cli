package com.atomist.rug.cli.command.shell;

import java.util.Arrays;
import java.util.List;

import com.atomist.rug.cli.command.AbstractCommandInfo;

public class HelpCommandInfo extends AbstractCommandInfo {

    public HelpCommandInfo() {
        super(HelpCommand.class, "help");
    }

    @Override
    public String description() {
        return "Print usage help";
    }

    @Override
    public String detail() {
        return "Prints this usage help.";
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE - 10;
    }

    @Override
    public String usage() {
        return "help";
    }

    @Override
    public String group() {
        return "6";
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList(new String[] { "h", "?" });
    }
}
