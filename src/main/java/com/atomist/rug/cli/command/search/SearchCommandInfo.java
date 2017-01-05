package com.atomist.rug.cli.command.search;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractRugScopedCommandInfo;

public class SearchCommandInfo extends AbstractRugScopedCommandInfo {

    public SearchCommandInfo() {
        super(SearchCommand.class, "search");
    }

    @Override
    public String description() {
        return "Search online catalog of available archives";
    }

    @Override
    public String detail() {
        return "SEARCH could be any text used to search the catalog.  TAG can be any valid tag, eg. spring or elm";
    }

    @Override
    public Options options() {
        Options options = super.options();
        options.addOption(Option.builder("T").argName("TAG").hasArg(true)
                .desc("Specify a TAG to filter search").longOpt("tag").optionalArg(true).build());
        return options;
    }

    @Override
    public int order() {
        return -10;
    }

    @Override
    public String usage() {
        return "search [OPTION]... [SEARCH]";
    }
}
