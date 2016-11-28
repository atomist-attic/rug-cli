package com.atomist.rug.cli.command;

import org.apache.commons.cli.Options;

public interface CommandInfo {

    String className();

    String description();

    String detail();

    Options globalOptions();

    String name();

    default Options options() {
        return new Options();
    }

    default int order() {
        // Per default commands to the end of the list
        return Integer.MAX_VALUE;
    }

    String usage();
}
