package com.atomist.rug.cli.command.path;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractRugScopedCommandInfo;

import java.util.Arrays;
import java.util.List;

public class PathCommandInfo extends AbstractRugScopedCommandInfo {

    private static final List<String> aliases = Arrays.asList(new String[] { "tree" });

    public PathCommandInfo() {
        super(PathCommand.class, "path");
    }

    @Override
    public String description() {
        return "Evaluate a path expression against a project";
    }

    @Override
    public String detail() {
        return "EXPRESSION can be any valid Rug path expression.  Depending on your expression you might need to put it in quotes.  "
                + "Use '--values' to display values of tree nodes; caution as this option might lead to a lot of data being printed.";
    }

    @Override
    public Options options() {
        Options options = super.options();
        options.addOption(Option.builder("C").longOpt("change-dir").argName("DIR").hasArg(true)
                .desc("Evaluate expression against project in directory DIR, default is '.'")
                .build());
        options.addOption("v", "values", false, "Displays tree node values");
        return options;
    }

    @Override
    public int order() {
        return -20;
    }

    @Override
    public String usage() {
        return "path [OPTION]... [EXPRESSION]";
    }

    @Override
    public String group() {
        return "3";
    }

    @Override
    public List<String> aliases() {
        return aliases;
    }
}
