package com.atomist.rug.cli.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.atomist.rug.cli.settings.SettingsReader;

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
        else if (!"settings".equals(opt)) {
            Map<String, Object> options = SettingsReader.read().getConfigValue("options",
                    Collections.emptyMap());
            if (options.containsKey(opt)) {
                return Optional.ofNullable(toString(options.get(opt)));
            }
            String o = opt.replace('-', '_');
            if (options.containsKey(o)) {
                return Optional.ofNullable(toString(options.get(o)));
            }
        }
        return Optional.empty();
    }

    public static boolean hasOption(String opt) {
        if (options != null && options.get() != null
                && options.get().stream().anyMatch(o -> o.getLongOpt().equals(opt)
                        || (o.getOpt() != null && o.getOpt().equals(opt)))) {
            return true;
        }
        else if (!"settings".equals(opt)) {
            Map<String, Object> options = SettingsReader.read().getConfigValue("options",
                    Collections.emptyMap());
            if (options.containsKey(opt)) {
                return toBoolean(options.get(opt));
            }
            String o = opt.replace('-', '_');
            if (options.containsKey(o)) {
                return toBoolean(options.get(o));
            }
        }
        return false;
    }

    public static void set(CommandLine cmd) {
        options.set(Arrays.asList(cmd.getOptions()));
    }

    private static Boolean toBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        else if (obj instanceof String) {
            String s = (String) obj;
            return "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
        }
        else if (obj instanceof Integer) {
            Integer i = (Integer) obj;
            return i == 1;
        }
        return false;
    }

    private static String toString(Object obj) {
        if (obj != null) {
            return obj.toString();
        }
        else {
            return null;
        }
    }
}
