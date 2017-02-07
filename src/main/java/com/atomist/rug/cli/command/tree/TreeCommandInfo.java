package com.atomist.rug.cli.command.tree;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractRugScopedCommandInfo;

public class TreeCommandInfo extends AbstractRugScopedCommandInfo {

    public TreeCommandInfo() {
        super(TreeCommand.class, "tree");
    }

    @Override
    public String description() {
        return "Evaluate a tree expression against a project";
    }

    @Override
    public String detail() {
        return "EXPRESSION can be any valid Rug tree expression.  Depending on your expression you might need to put it in quotes.";
    }

    @Override
    public Options options() {
        Options options = super.options();
        options.addOption(Option.builder("C").longOpt("change-dir").argName("DIR").hasArg(true)
                .desc("Evaluate expression against project in directory DIR, default is '.'")
                .required(false).build());
        return options;
    }

    @Override
    public int order() {
        return -20;
    }

    @Override
    public String usage() {
        return "tree [OPTION]... [EXPRESSION]";
    }
}
