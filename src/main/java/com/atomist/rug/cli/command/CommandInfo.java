package com.atomist.rug.cli.command;

import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.Options;

import com.atomist.rug.resolver.ArtifactDescriptor;

public interface CommandInfo {

    String className();

    String description();

    String detail();

    Options globalOptions();

    String name();

    String usage();
    
    default boolean enabled(ArtifactDescriptor artifact) {
        return true;
    }

    default Options options() {
        return new Options();
    }

    default int order() {
        // Per default commands to the end of the list
        return Integer.MAX_VALUE;
    }
    
    default boolean loadArtifactSource() {
        return true;
    }
    
    default List<String> subCommands() {
        return Collections.emptyList();
    }

    default List<String> aliases() {
        return Collections.emptyList();
    }
    
    default String group() {
        return "rug";
    }
}
