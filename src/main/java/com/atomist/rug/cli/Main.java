package com.atomist.rug.cli;

import com.atomist.rug.cli.command.ServiceLoadingCommandInfoRegistry;
import com.atomist.rug.cli.output.ConsoleUtils;

public class Main {

    public static void main(String[] args) throws Exception {
        configureEnv();
        configureStreams();

        new Runner(new ServiceLoadingCommandInfoRegistry()).run(args);
    }

    private static void configureStreams() {
        ConsoleUtils.configureStreams();
    }

    private static void configureEnv() {
        System.setProperty("java.awt.headless", Boolean.toString(true));
    }

}
