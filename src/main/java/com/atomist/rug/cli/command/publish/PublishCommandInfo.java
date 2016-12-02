package com.atomist.rug.cli.command.publish;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.atomist.rug.cli.command.AbstractLocalArtifactDescriptorProvider;
import com.atomist.rug.cli.command.CommandInfo;

public class PublishCommandInfo extends AbstractLocalArtifactDescriptorProvider
        implements CommandInfo {

    public PublishCommandInfo() {
        super(PublishCommand.class, "publish");
    }

    @Override
    public String description() {
        return "Create and publish an archive into a remote repository";
    }

    @Override
    public String detail() {
        return "Create a Rug archive from the current repo and publish it in a remote repository.  "
                + "Ensure that there is a manifest.yml descriptor in the .atomist directory.  "
                + "Use -i to specify what repository configuration should be used to publish.  "
                + "ID should refer to a repository name in cli.yml";
    }

    @Override
    public Options options() {
        Options options = new Options();
        options.addOption(Option.builder("a").longOpt("archive-version").argName("AV").hasArg(true)
                .required(false).desc("Override artifact version with AV").build());
        options.addOption(Option.builder("i").longOpt("id").argName("ID").hasArg(true)
                .required(false).desc("ID identifying the repository to publish into").build());

        return options;
    }
    
    @Override
    public int order() {
        return 70;
    }

    @Override
    public String usage() {
        return "publish [OPTION]...";
    }
}
