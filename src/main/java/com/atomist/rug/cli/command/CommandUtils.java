package com.atomist.rug.cli.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.*;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.utils.ParseExceptionProcessor;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.resolver.DependencyResolverFactory;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.resolver.ArtifactDescriptor.Extension;
import com.atomist.rug.resolver.DefaultArtifactDescriptor;
import com.atomist.rug.resolver.DependencyResolver;

public abstract class CommandUtils {

    private static final String RUG_VERSION = ".*<rug.version>(.*)<\\/rug.version>.*";

    public static File getRequiredWorkingDirectory() {
        Optional<File> projectDir = FileUtils.getWorkingDirectory();
        return projectDir.orElseThrow(() -> new CommandException(
                "Current directory is not a valid archive directory. Couldn't find .atomist folder."));
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("markdown")) {
                markdownPrint();
            } else if (args[0].equals("flat")) {
                flatPrint();
            } else {
                System.err.println("unknown print format: " + args[0]);
            }
        } else {
            flatPrint();
        }
    }

    private static void flatPrint() {
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

    private static void markdownPrint() {
        System.out.print("Below is the complete list of options and commands for the Rug CLI.\n\n");

        ServiceLoadingCommandInfoRegistry commandRegistry = new ServiceLoadingCommandInfoRegistry();
        CommandInfo helpCmdInfo = commandRegistry.findCommand("help");
        Collection<Option> globalOptions = helpCmdInfo.globalOptions().getOptions();
        if (globalOptions != null && globalOptions.size() > 0) {
            System.out.print("## Global command-line options\n\n");
            globalOptions.forEach(o -> System.out.print(formatOption(o)));
        }

        List<CommandInfo> commandInfos = commandRegistry.commands();
        if (commandInfos != null && commandInfos.size() > 0) {
            System.out.print("## Commands\n\n");
            Collections.sort(commandInfos, Comparator.comparing(CommandInfo::name));
            commandInfos.forEach(c -> formatCommand(c, "###"));
        }
    }

    /**
     * This is truly horrible.
     */
    private static void formatCommand(CommandInfo c, String header) {
        System.out.print(header + " `" + c.name() + "`\n\n");
        System.out.print(c.description() + "\n\n");

        String cmdUsage = c.usage();
        if (cmdUsage != null && cmdUsage.length() > 0) {
            System.out.print("**Usage:**\n\n");
            System.out.print("```console\n$ rug " + cmdUsage + "\n```\n\n");
        }

        String cmdDetail = c.detail();
        if (cmdDetail != null && cmdDetail.length() > 0) {
            System.out.print(cmdDetail + "\n\n");
        }

        List<String> cmdAliases = c.aliases();
        if (cmdAliases != null && cmdAliases.size() > 0) {
            System.out.print("**Command aliases:** `" + String.join("`, `", cmdAliases) + "`\n\n");
        }

        List<String> subCmds = c.subCommands();
        if (subCmds != null && subCmds.size() > 0) {
            System.out.print("**Subcommands:** `" + String.join("`, `", subCmds) + "`\n\n");
        }

        Collection<Option> cmdOptions = c.options().getOptions();
        if (cmdOptions != null && cmdOptions.size() > 0) {
            System.out.print("**Command options:**\n\n");
            List<Option> cmdOptionsArray = new ArrayList<>(cmdOptions);
            Collections.sort(cmdOptionsArray, (Option a, Option b) -> {
                String aShort = a.getOpt();
                String bShort = b.getOpt();
                String aLong = a.getLongOpt();
                String bLong = b.getLongOpt();
                if (aShort != null && aShort.length() > 0 && bShort != null && bShort.length() > 0) {
                    return aShort.compareToIgnoreCase(bShort);
                } else if (aLong != null && aLong.length() > 0 && bLong != null && bLong.length() > 0) {
                    return aLong.compareToIgnoreCase(bLong);
                } else if (aShort != null && aShort.length() > 0 && bLong != null && bLong.length() > 0) {
                    return aShort.compareToIgnoreCase(bLong);
                } else if (aLong != null && aLong.length() > 0 && bShort != null && bShort.length() > 0) {
                    return aLong.compareToIgnoreCase(bShort);
                } else {
                    return 0;
                }
            });
            cmdOptionsArray.forEach(o -> System.out.print(formatOption(o)));
        }
    }

    /**
     * Oh yeah, it's not as bad as this!
     *
     * @param o Option to be formatted
     * @return Markdown formatted string
     */
    private static String formatOption(Option o) {
        String shortString = "";
        String shortOpt = o.getOpt();
        if (shortOpt != null && shortOpt.length() > 0) {
            shortString = "-" + shortOpt;
        }
        String longString = "";
        String longOpt = o.getLongOpt();
        if (longOpt != null && longOpt.length() > 0) {
            longString = "--" + longOpt;
        }
        String joinString = "";
        if (shortString.length() > 0 && longString.length() > 0) {
            joinString = ",";
        }
        String argName = o.getArgName();
        if (argName != null && argName.length() > 0) {
            if (shortString.length() > 0) {
                shortString += " " + argName;
            }
            if (longString.length() > 0) {
                longString += "=" + argName;
            }
        }
        if (shortString.length() > 0) {
            shortString = "`" + shortString + "`";
        }
        if (longString.length() > 0) {
            longString = "`" + longString + "`";
        }
        // This formatting uses the MkDocs def_list markdown extension
        return String.format("%s%s%s\n:   %s\n\n", shortString, joinString, longString, o.getDescription());
    }

    public static Options options() {
        Options options = new Options();
        options.addOption("v", "version", false, "Print version information");
        options.addOption("?", "help", false, "Print help information");
        options.addOption("h", "help", false, "Print help information");
        return options;
    }

    public static CommandLine parseInitialCommandline(String[] args, CommandInfoRegistry registry) {
        try {
            // For the purpose of the initial parse we need to collect all options and make sure
            // they are not configured with required=true and then parse the commandLine
            Options options = new Options();
            registry.allOptions().getOptions()
                    .forEach(o -> options.addOption(o.getOpt(), o.getLongOpt(), o.hasArg(), null));

            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args, true);
            CommandLineOptions.set(commandLine);
            return commandLine;
        }
        catch (ParseException e) {
            throw new CommandException(ParseExceptionProcessor.process(e), (String) null);
        }
    }

    public static CommandLine parseCommandline(String commandName, String[] args,
            CommandInfoRegistry registry) {
        args = firstCommand(args);
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(registry.options(commandName), args);
            CommandLineOptions.set(commandLine);
            return commandLine;
        }
        catch (ParseException e) {
            throw new CommandException(ParseExceptionProcessor.process(e), (String) null);
        }
    }

    public static String[] firstCommand(String[] args) {
        // make sure to only look at the current command; that is the first of potentially many
        // concatinated with &&
        List<String> parts = Arrays.asList(args);
        if (parts.indexOf("&&") >= 0) {
            args = parts.subList(0, parts.indexOf("&&")).toArray(new String[0]);
        }
        return args;
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
        // The following can really only happen during CLI development when we have a version range
        // in the pom.xml
        if (version.startsWith("(")) {
            ArtifactDescriptor artifact = new DefaultArtifactDescriptor(Constants.GROUP,
                    Constants.RUG_ARTIFACT, version, Extension.JAR);
            version = new ProgressReportingOperationRunner<String>(
                    String.format("Resolving version range for %s",
                            ArtifactDescriptorUtils.coordinates(artifact))).run((indicator) -> {
                                DependencyResolver resolver = DependencyResolverFactory
                                        .createDependencyResolver(artifact, indicator);
                                return resolver.resolveVersion(artifact);
                            });
        }
        return version;
    }
}
