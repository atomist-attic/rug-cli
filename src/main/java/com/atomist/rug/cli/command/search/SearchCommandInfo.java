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
        return "SEARCH could be any text used to search the catalog.  TAG can be any valid tag, eg. spring or elm.  TYPE can be either 'editor', 'generator', 'executor' or 'reviewer'.";
    }

    @Override
    public Options options() {
        Options options = super.options();
        options.addOption(Option.builder("T").argName("TAG").hasArg(true)
                .desc("Specify a TAG to filter search").longOpt("tag").optionalArg(true).build());
        options.addOption(Option.builder().argName("TYPE").hasArg(true)
                .desc("Specify a TYPE to filter search based on Rug type").longOpt("type")
                .optionalArg(true).build());
        options.addOption(Option.builder().hasArg(false).desc("Show operations in search output")
                .longOpt("operations").optionalArg(true).build());
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

    @Override
    public String group() {
        return "1";
    }
}
