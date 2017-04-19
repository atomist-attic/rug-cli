package com.atomist.rug.cli.command.dependencies;

import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractVersionCommandInfo;
import com.atomist.rug.cli.command.CommandInfo;

public class DependenciesCommandInfo extends AbstractVersionCommandInfo implements CommandInfo {

    public DependenciesCommandInfo() {
        super(DependenciesCommand.class, "dependencies", 1);
    }

    @Override
    public String description() {
        return "Print dependency tree for an archive";
    }

    @Override
    public String detail() {
        return "ARTIFACT should be the full name of an artifact, e.g., \"atomist:spring-service:Spring Microservice\".  "
                + "If the name of the artifact has spaces in it, you need to put quotes around it.";
    }

    @Override
    public Options options() {
        Options options = super.options();
        options.addOption(Option.builder("O").hasArg(false).desc("List operations")
                .longOpt("operations").optionalArg(true).build());
        return options;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public String usage() {
        return "dependencies [OPTION]... ARTIFACT";
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("deps");
    }

    @Override
    public String group() {
        return "1";
    }
}
