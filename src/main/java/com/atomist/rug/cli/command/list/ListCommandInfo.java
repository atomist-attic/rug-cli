package com.atomist.rug.cli.command.list;

import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractRugScopedCommandInfo;

public class ListCommandInfo extends AbstractRugScopedCommandInfo {

    public ListCommandInfo() {
        super(ListCommand.class, "list");
    }

    @Override
    public String description() {
        return "List locally installed archives";
    }

    @Override
    public String detail() {
        return "FILTER could be any of group, artifact or version.  VALUE should be a valid filter "
                + "expression: for group and artifact ? and * are supported as wildcards;  the version "
                + "filter can be any valid version or version range.";
    }

    @Override
    public Options options() {
        Options options = super.options();
        options.addOption(Option.builder("f").argName("FILTER=VALUE").numberOfArgs(2)
                .valueSeparator('=').desc("Specify filter of type FILTER with VALUE")
                .longOpt("filter").optionalArg(true).build());
        return options;
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public String usage() {
        return "list [OPTION]...";
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("ls");
    }

    @Override
    public String group() {
        return "1";
    }
}
