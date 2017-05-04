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
        options.addOption("?", "help", false, "Print usage help");
        options.addOption("h", "help", false, "Print usage help");
        options.addOption("X", "error", false, "Print stacktraces");
        options.addOption("V", "verbose", false, "Print verbose output");
        options.addOption(Option.builder("s").longOpt("settings").argName("FILE").hasArg(true)
                .required(false).desc("Use settings file FILE").build());
        options.addOption("q", "quiet", false, "Do not display progress messages");
        options.addOption("n", "noisy", false, "Display more progress messages");
        options.addOption("o", "offline", false, "Use only downloaded archives");
        options.addOption("t", "timer", false, "Print timing information");
        options.addOption("r", "resolver-report", false, "Print dependency tree");
        options.addOption("u", "update", false, "Update dependency resolution");
        options.addOption(Option.builder().longOpt("requires").argName("RUG_VERSION").hasArg(true)
                .required(false).desc("Overwrite the Rug version to RUG_VERSION (Use with Caution)")
                .build());
        options.addOption(Option.builder().longOpt("disable-verification").hasArg(false)
                .required(false).desc("Disable verification of extensions (Use with Caution)")
                .build());
        options.addOption(Option.builder().longOpt("disable-version-check").hasArg(false)
                .required(false).desc("Disable version compatibility check (Use with Caution)")
                .build());
        return options;
    }

    @Override
    public String name() {
        return commandName.replace('_', ' ');
    }
}
