package com.atomist.rug.cli.command.edit;

import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractVersionCommandInfo;

public class EditCommandInfo extends AbstractVersionCommandInfo {

    public EditCommandInfo() {
        super(EditCommand.class, "edit", 1);
    }

    @Override
    public String description() {
        return "Run an editor to modify an existing project";
    }

    @Override
    public String detail() {
        return "EDITOR is a Rug editor, e.g., \"atomist:common-editors:AddReadme\".  If the name of "
                + "the editor has spaces in it, you need to put quotes around it.  To pass parameters to the "
                + "editor you can specify multiple PARAMETERs in \"form NAME=VALUE\".";
    }

    @Override
    public Options options() {
        Options options = super.options();
        options.addOption(Option.builder("C").longOpt("change-dir").argName("DIR").hasArg(true)
                .desc("Run editor in directory DIR, default is '.'").required(false).build());
        options.addOption("d", "dry-run", false, "Do not persist changes, print diffs");
        options.addOption("R", "repo", false, "Commit files to local git repository");
        options.addOption("I", "interactive", false,
                "Interactive mode for specifying parameter values");
        return options;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public String usage() {
        return "edit [OPTION]... EDITOR [PARAMETER]...";
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("ed");
    }

    @Override
    public String group() {
        return "2";
    }
}
