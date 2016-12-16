package com.atomist.rug.cli.command.extension;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractRugScopedCommandInfo;

public class ExtensionCommandInfo extends AbstractRugScopedCommandInfo {

    public ExtensionCommandInfo() {
        super(ExtensionCommand.class, "extension");
    }

    @Override
    public String description() {
        return "Manage command line extensions";
    }

    @Override
    public String detail() {
        return "SUBCOMMAND is either install, uninstall or list.  EXTENSION should be a valid extension"
                + " identifier of form GROUP:ARTIFACT.  If no version EV is provided with -a, the "
                + "latest version of the extension is installed.";
    }

    @Override
    public String usage() {
        return "extension SUBCOMMAND [OPTION]... [EXTENSION]";
    }
    
    @Override
    public int order() {
        return 90;
    }

    @Override
    public Options options() {
        Options options = new Options();
        options.addOption(Option.builder("a").longOpt("extension-version").argName("EV")
                .hasArg(true).required(false).desc("Version EV of extension to install").build());
        return options;
    }
}
