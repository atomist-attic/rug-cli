package com.atomist.rug.cli.command.generate;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractVersionCommandInfo;

public class GenerateCommandInfo extends AbstractVersionCommandInfo {

    public GenerateCommandInfo() {
        super(GenerateCommand.class, "generate", 1);
    }

    @Override
    public String description() {
        return "Run a generator to create a new project";
    }

    @Override
    public String detail() {
        return "GENERATOR is a Rug generator, e.g., \"atomist:spring-service:Spring Microservice\".  "
                + "If the name of the generator has spaces in it, you need to put quotes around it.  "
                + "PROJECT_NAME specifies the required name of the generated project.  "
                + "To pass parameters to the generator you can specify multiple PARAMETERs in form \"NAME=VALUE\".";
    }

    @Override
    public Options options() {
        Options options = super.options();
        options.addOption("R", "repo", false,
                "Initialize and commit files to a new git repository");
        options.addOption("F", "overwrite", false,
                "Force overwrite if target directory already exists");
        options.addOption(Option.builder("C").longOpt("change-dir").argName("DIR")
                .desc("Create project in directory DIR, default is '.'").hasArg(true)
                .required(false).build());
        return options;
    }

    @Override
    public int order() {
        return 20;
    }
    
    @Override
    public String usage() {
        return "generate [OPTION]... GENERATOR PROJECT_NAME [PARAMETER]...";
    }
}
