package com.atomist.rug.cli.command;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public abstract class AbstractCommandInfo implements CommandInfo {

    protected final String className;
    protected final String commandName;

    public AbstractCommandInfo(Class<? extends Command> commandClass, String commandName) {
        this.className = commandClass.getName();
        this.commandName = commandName;
    }

    @Override
    public String className() {
        return this.className;
    }

    @Override
    public final Options globalOptions() {
        Options options = new Options();
        options.addOption("?", "help", false, "Print help information");
        options.addOption("h", "help", false, "Print help information");
        options.addOption("X", "error", false, "Print verbose error messages");
        options.addOption(Option.builder("s").longOpt("settings").argName("FILE").hasArg(true)
                .required(false).desc("Use settings file FILE").build());
        options.addOption("q", "quiet", false, "Do not display progress messages");
        options.addOption("o", "offline", false, "Use only downloaded archives");
        options.addOption("t", "timer", false, "Print timing information");
        options.addOption("r", "resolver-report", false, "Print dependency tree");
        options.addOption("u", "update", false, "Update dependency resolution");

        return options;
    }

    @Override
    public String name() {
        return commandName;
    }
}
