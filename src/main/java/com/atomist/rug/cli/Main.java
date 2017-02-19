package com.atomist.rug.cli;

import java.io.IOException;
import java.util.Arrays;

import com.atomist.rug.cli.command.ServiceLoadingCommandInfoRegistry;
import com.atomist.rug.cli.output.ConsoleUtils;

/**
 * Main entry point into the CLI
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // For attaching a profiler, we are waiting for user input when -w is specified on the
        // commandline
        args = waitForInput(args);

        // Some setup
        configureEnv();

        // Wrap streams so that we can intercept calls to System.out.println etc
        configureStreams();

        invokeRunner(args);
    }

    private static void invokeRunner(String[] args) {
        boolean shouldContinue = true;
        
        while (shouldContinue) {
            try {
                shouldContinue = false;
                new Runner(new ServiceLoadingCommandInfoRegistry()).run(args);
            }
            catch (ReloadException e) {
                args = e.args();
                shouldContinue = true;
            }
        }
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
