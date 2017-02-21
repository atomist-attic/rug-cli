package com.atomist.rug.cli.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.atomist.rug.cli.command.utils.ParseExceptionProcessor;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.FileUtils;

public abstract class CommandUtils {

    private static final String RUG_VERSION = ".*<rug.version>(.*)<\\/rug.version>.*";

    public static File getRequiredWorkingDirectory() {
        Optional<File> projectDir = FileUtils.getWorkingDirectory();
        return projectDir.orElseThrow(() -> new CommandException(
                "Current directory is not a valid archive directory. Couldn't find .atomist folder."));
    }

    public static void main(String[] args) {
        new ServiceLoadingCommandInfoRegistry().commands().forEach(c -> {
            System.out.println(c.name());
            System.out.println("");
            c.globalOptions().getOptions().forEach(o -> System.out
                    .println(String.format("  -%s,--%s", o.getOpt(), o.getLongOpt())));
            System.out.println("");
            c.options().getOptions().forEach(o -> System.out
                    .println(String.format("  -%s,--%s", o.getOpt(), o.getLongOpt())));
            System.out.println("");
        });
    }

    public static Options options() {
        Options options = new Options();
        options.addOption("v", "version", false, "Print version information");
        options.addOption("?", "help", false, "Print help information");
        options.addOption("h", "help", false, "Print help information");
        options.addOption("q", "quiet", false, "Do not display progress messages");
        return options;
    }

    public static CommandLine parseCommandline(String[] args, CommandInfoRegistry registry) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(registry.allOptions(), args);
            CommandLineOptions.set(commandLine);
            return commandLine;
        }
        catch (ParseException e) {
            throw new CommandException(ParseExceptionProcessor.process(e), (String) null);
        }
    }

    public static String[] splitCommandline(String toProcess) {
        if (toProcess == null || toProcess.length() == 0) {
            // no command? no string
            return new String[0];
        }
        // parse with a simple finite state machine

        int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
            case inQuote:
                if ("\'".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                }
                else {
                    current.append(nextTok);
                }
                break;
            case inDoubleQuote:
                if ("\"".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                }
                else {
                    current.append(nextTok);
                }
                break;
            default:
                if ("\'".equals(nextTok)) {
                    state = inQuote;
                }
                else if ("\"".equals(nextTok)) {
                    state = inDoubleQuote;
                }
                else if (" ".equals(nextTok)) {
                    if (lastTokenHasBeenQuoted || current.length() != 0) {
                        result.add(current.toString());
                        current.setLength(0);
                    }
                }
                else {
                    current.append(nextTok);
                }
                lastTokenHasBeenQuoted = false;
                break;
            }
        }
        if (lastTokenHasBeenQuoted || current.length() != 0) {
            result.add(current.toString());
        }
        if (state == inQuote || state == inDoubleQuote) {
            throw new RuntimeException("unbalanced quotes in " + toProcess);
        }
        return result.toArray(new String[result.size()]);
    }

    public static String readRugVersionFromPom() {
        String version = "latest";
        Pattern pattern = Pattern.compile(RUG_VERSION);
        try (InputStream is = AbstractRugScopedCommandInfo.class.getClassLoader()
                .getResourceAsStream("META-INF/maven/com.atomist/rug-cli/pom.xml")) {
            if (is != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = in.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        version = matcher.group(1);
                        break;
                    }
                }
            }
        }
        catch (IOException e) {
            // just use latest as fallback
        }
        return version;
    }
}
