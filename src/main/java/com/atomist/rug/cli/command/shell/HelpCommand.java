package com.atomist.rug.cli.command.shell;

import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.cli.command.ServiceLoadingCommandInfoRegistry;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.utils.CommandHelpFormatter;

public class HelpCommand extends AbstractAnnotationBasedCommand {

    @Command
    public void run() {
        log.info(new CommandHelpFormatter().printHelp(new ServiceLoadingCommandInfoRegistry(),
                CommandUtils.options()));
    }
}
