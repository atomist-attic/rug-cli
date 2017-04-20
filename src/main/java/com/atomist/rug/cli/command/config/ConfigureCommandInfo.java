package com.atomist.rug.cli.command.config;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractRugScopedCommandInfo;

public class ConfigureCommandInfo extends AbstractRugScopedCommandInfo {

    private static final List<String> commands = Arrays.asList("default archive", "repositories");

    public ConfigureCommandInfo() {
        super(ConfigureCommand.class, "configure");
    }

    @Override
    public String description() {
        return "Change/manage configuration settings";
    }

    @Override
    public String detail() {
        return "SUBCOMMAND can either be default archive or repositories.  The repositories command "
                + "uses your GitHub authentication to configure all of your "
                + "private Rug archive repositories and enables them for publication with the publish "
                + "command.  Please execute the login command before configuring repositories."
                + "The default archive command sets a global or project specific Rug archive so "
                + "that Rugs can be invoked without a fully qualified coordinate.  ARCHIVE should be"
                + " a valid archive coordinate of form GROUP:ARTIFACT or just GROUP.  At any time "
                + "those defaults can be overriden by specifying GROUP:ARTIFACT and -a from the "
                + "command line.";
    }

    @Override
    public Options options() {
        Options options = new Options();
        options.addOption(
                Option.builder("a").argName("AV").desc("Set default archive version to AV")
                        .longOpt("archive-version").hasArg(true).optionalArg(true).build());
        options.addOption(Option.builder("g").desc("Set global or project default archive")
                .longOpt("global").hasArg(false).build());
        options.addOption(Option.builder("S").desc("Set default archive")
                .longOpt("save").hasArg(false).build());
        options.addOption(Option.builder("D").desc("Remove default archive")
                .longOpt("delete").hasArg(false).build());
        return options;
    }

    @Override
    public int order() {
        return 80;
    }

    @Override
    public String usage() {
        return "configure [OPTION]... SUBCOMMAND [ARCHIVE]";
    }

    @Override
    public List<String> subCommands() {
        return commands;
    }

    @Override
    public String group() {
        return "4";
    }
    
    @Override
    public List<String> aliases() {
        return Arrays.asList(new String[] {"config", "conf"});
    }
}
