package com.atomist.rug.cli;

import java.io.IOException;
import java.util.Arrays;

import com.atomist.rug.cli.command.ServiceLoadingCommandInfoRegistry;
import com.atomist.rug.cli.output.ConsoleUtils;

public class Main {

    public static void main(String[] args) throws Exception {
        args = waitForInput(args);
        
        configureEnv();
        configureStreams();

        new Runner(new ServiceLoadingCommandInfoRegistry()).run(args);
    }

    private static String[] waitForInput(String[] args) throws IOException {
        if (args.length > 0 && args[args.length - 1].equals("-w")) {
            System.in.read();
            args = Arrays.copyOfRange(args, 0, args.length - 1);
        }
        return args;
    }

    private static void configureEnv() {
        System.setProperty("java.awt.headless", Boolean.toString(true));
    }

    private static void configureStreams() {
        ConsoleUtils.configureStreams();
    }


}
