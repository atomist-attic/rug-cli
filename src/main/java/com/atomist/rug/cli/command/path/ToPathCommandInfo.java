package com.atomist.rug.cli.command.path;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractRugScopedCommandInfo;

public class ToPathCommandInfo extends AbstractRugScopedCommandInfo {

    private static final List<String> aliases = Arrays.asList(new String[] { "to-tree" });

    public ToPathCommandInfo() {
        super(ToPathCommand.class, "to-path");
    }

    @Override
    public String description() {
        return "Display path expression to a point in a file within a project";
    }

    @Override
    public String detail() {
        return "PATH must be a valid path within the project at DIR or '.'.  ";
    }

    @Override
    public Options options() {
        Options options = super.options();
        options.addOption(Option.builder("C").longOpt("change-dir").argName("DIR").hasArg(true)
                .desc("Evaluate expression against project in directory DIR, default is '.'")
                .build());
        options.addOption(Option.builder().argName("KIND")
                .desc("Rug Extension kind, eg. 'ScalaFile' or 'Pom'").longOpt("kind")
                .optionalArg(false).hasArg(true).build());
        options.addOption(Option.builder().argName("LINE").desc("Line within the file")
                .longOpt("line").optionalArg(false).hasArg(true).build());
        options.addOption(Option.builder().argName("COLUMN").desc("Column within file at LINE")
                .longOpt("column").optionalArg(false).hasArg(true).build());
        return options;
    }

    @Override
    public int order() {
        return -20;
    }

    @Override
    public String usage() {
        return "to-path [OPTION]... PATH";
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
