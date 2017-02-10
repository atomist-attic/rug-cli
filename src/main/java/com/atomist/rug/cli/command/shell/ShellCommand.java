package com.atomist.rug.cli.command.shell;

import org.apache.commons.lang3.StringUtils;

import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.version.VersionUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;

public class ShellCommand extends AbstractAnnotationBasedCommand {

    private static final String banner = "  ____                 ____ _     ___ \n" + 
            " |  _ \\ _   _  __ _   / ___| |   |_ _|\n" + 
            " | |_) | | | |/ _` | | |   | |    | | \n" + 
            " |  _ <| |_| | (_| | | |___| |___ | | \n" + 
            " |_| \\_\\\\__,_|\\__, |  \\____|_____|___|\n" + 
            "              |___/ %s";
    
    @Command
    public void run(ArtifactDescriptor artifact) {
        
        // Maybe this is a good place to set up state for the current shell instance
        log.newline();
        log.info(banner, StringUtils.leftPad(VersionUtils.readVersion().orElse("0.0.0").split("-")[0], 18));
    }

}
