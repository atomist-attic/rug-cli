package com.atomist.rug.cli.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

// TODO rename to CommandLineHolder
public abstract class CommandLineOptions {

    private static InheritableThreadLocal<List<Option>> options = new InheritableThreadLocal<>();

    public static Optional<String> getOptionValue(String opt) {
        Optional<Option> option = options.get().stream().filter(
                o -> o.getLongOpt().equals(opt) || (o.getOpt() != null && o.getOpt().equals(opt)))
                .findFirst();
        if (option.isPresent()) {
            return Optional.of(option.get().getValue());
        }
        else {
            return Optional.empty();
        }
    }

    public static boolean hasOption(String opt) {
        return options != null && options.get() != null && options.get().stream().anyMatch(
                o -> o.getLongOpt().equals(opt) || (o.getOpt() != null && o.getOpt().equals(opt)));
    }

    public static void set(CommandLine commandLine) {
        options.set(Arrays.asList(commandLine.getOptions()));
    }

}
