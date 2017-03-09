package com.atomist.rug.cli.command;

import org.apache.commons.cli.Option;

import java.util.*;

public class MkDocs {

    public static void main(String[] args) {
        markdownPrint();
    }

    private static void markdownPrint() {
        ServiceLoadingCommandInfoRegistry commandInfoRegistry = new ServiceLoadingCommandInfoRegistry();
        System.out.print(markdownDocs(commandInfoRegistry));
    }

    protected static String markdownDocs(ServiceLoadingCommandInfoRegistry commandInfoRegistry) {
        StringBuilder markdown =
                new StringBuilder("Below is the complete list of options and commands for the Rug CLI.\n\n");

        List<CommandInfo> commandInfos = commandInfoRegistry.commands();

        if (commandInfos != null && commandInfos.size() > 0) {
            CommandInfo someCmdInfo = commandInfos.get(0);
            Collection<Option> globalOptions = someCmdInfo.globalOptions().getOptions();
            if (globalOptions != null && globalOptions.size() > 0) {
                markdown.append("## Global command-line options\n\n");
                List<Option> globalOptionsArray = new ArrayList<>(globalOptions);
                Collections.sort(globalOptionsArray, MkDocs::compareOptions);
                globalOptionsArray.forEach(o -> markdown.append(formatOption(o)));
            }

            markdown.append("## Commands\n\n");
            Collections.sort(commandInfos, Comparator.comparing(CommandInfo::name));
            commandInfos.forEach(c -> markdown.append(formatCommand(c, "###")));
        }

        return markdown.toString();
    }

    protected static int compareOptions(Option a, Option b) {
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
    }

    /**
     * This is truly horrible.
     *
     * @param c       Command to be formatted.
     * @param header  Current header level represented as string of hash marks, e.g., "##".
     * @return        Markdown formatted documentation string.
     */
    protected static String formatCommand(CommandInfo c, String header) {
        StringBuilder cmdMarkdown = new StringBuilder();

        cmdMarkdown.append(header + " `" + c.name() + "`\n\n");
        cmdMarkdown.append(c.description() + "\n\n");

        String cmdUsage = c.usage();
        if (cmdUsage != null && cmdUsage.length() > 0) {
            cmdMarkdown.append("**Usage:**\n\n");
            cmdMarkdown.append("```console\n$ rug " + cmdUsage + "\n```\n\n");
        }

        String cmdDetail = c.detail();
        if (cmdDetail != null && cmdDetail.length() > 0) {
            cmdMarkdown.append(cmdDetail + "\n\n");
        }

        List<String> cmdAliases = c.aliases();
        if (cmdAliases != null && cmdAliases.size() > 0) {
            cmdMarkdown.append("**Command aliases:** `" + String.join("`, `", cmdAliases) + "`\n\n");
        }

        List<String> subCmds = c.subCommands();
        if (subCmds != null && subCmds.size() > 0) {
            cmdMarkdown.append("**Subcommands:** `" + String.join("`, `", subCmds) + "`\n\n");
        }

        Collection<Option> cmdOptions = c.options().getOptions();
        if (cmdOptions != null && cmdOptions.size() > 0) {
            cmdMarkdown.append("**Command options:**\n\n");
            List<Option> cmdOptionsArray = new ArrayList<>(cmdOptions);
            Collections.sort(cmdOptionsArray, MkDocs::compareOptions);
            cmdOptionsArray.forEach(o -> cmdMarkdown.append(formatOption(o)));
        }

        return cmdMarkdown.toString();
    }

    /**
     * Oh yeah?  It's not as bad as this!
     *
     * @param o  Option to be formatted.
     * @return   Markdown formatted documentation string.
     */
    protected static String formatOption(Option o) {
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
            joinString = ", ";
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
}
