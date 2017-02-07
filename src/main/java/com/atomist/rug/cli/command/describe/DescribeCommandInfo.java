package com.atomist.rug.cli.command.describe;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractVersionCommandInfo;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.CommandInfo;
import com.atomist.rug.resolver.ArtifactDescriptor;

public class DescribeCommandInfo extends AbstractVersionCommandInfo implements CommandInfo {

    private static List<String> commands = Arrays
            .asList("editor", "generator", "executor", "reviewer", "archive");

    public DescribeCommandInfo() {
        super(DescribeCommand.class, "describe", 2);
    }

    @Override
    public ArtifactDescriptor artifactDescriptor(CommandLine commandLine) {
        if (commandLine.hasOption('l')) {
            return super.artifactDescriptor(commandLine);
        }

        String version = commandLine.getOptionValue('a');

        List<String> args = commandLine.getArgList();
        if (args.size() == 3) {
            // describe editor atomist:common-editors
            return findArtifact(args.get(2), version);
        }
        else if (args.size() == 2) {
            String second = args.get(1);
            // describe editor
            if (commands.contains(second)) {
                return findArtifact(null, version);
            }
            // describe atomist:common-editors
            else {
                return findArtifact(second, version);
            }
        }
        else if (args.size() == 1) {
            // describe
            return findArtifact(null, version);
        }
        throw new CommandException("No TYPE provided.", "describe");
    }

    @Override
    public String description() {
        return "Print details about an archive or Rug";
    }

    @Override
    public String detail() {
        return "TYPE should be 'editor', 'generator', 'executor', 'reviewer' or 'archive' and ARTIFACT"
                + " should be the full name of an artifact, e.g., \"atomist:spring-service:Spring Microservice\".  "
                + "If the name of the artifact has spaces in it, you need to put quotes around it.  "
                + "FORMAT can be 'json' or 'yaml' and is only valid when describing an archive.";
    }
    
    public Options options() {
        Options options = super.options();
        options.addOption(Option.builder("O").argName("FORMAT").desc("Specify output FORMAT")
                .longOpt("output").hasArg(true).required(false).build());
        return options;
    }

    @Override
    public int order() {
        return 10;
    }
    
    @Override
    public String usage() {
        return "describe [OPTION]... TYPE ARTIFACT";
    }
}
