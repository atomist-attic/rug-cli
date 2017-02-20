package com.atomist.rug.cli.command;

import com.atomist.rug.cli.Constants;

public class CommandException extends RuntimeException {

    private static final long serialVersionUID = -2519146217773751039L;

    public CommandException(String msg) {
        super(msg);
    }

    public CommandException(String msg, Throwable e) {
        super(msg, e);
    }

    public CommandException(String msg, String command) {
        super(msg + getHelpText(command));
    }

    private static String getHelpText(String command) {
        if (command == null) {
            return String.format("\n\nRun the following command for usage help:\n  %shelp",
                    Constants.command());
        }
        else {
            return String.format("\n\nRun the following command for usage help:\n  %s%s --help",
                    Constants.command(), command);
        }
    }

}
