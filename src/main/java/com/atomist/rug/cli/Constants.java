package com.atomist.rug.cli;

import org.apache.commons.lang3.SystemUtils;

import com.atomist.rug.cli.version.VersionUtils;

public class Constants {
    
    public static final String COMMAND = "rug";
    public static final String GROUP = "com.atomist";
    public static final String ARTIFACT = "rug-cli";
    
    public static final String RUG_ARTIFACT = "rug";
    public static final String RUG_VERSION_RANGE = "[0.7.0,1.0.0)";

    public static final String ATOMIST_ROOT = ".atomist";
    public static final String CLI_CONFIG_NAME = "cli.yml";
    
    public static final String DIVIDER = (SystemUtils.IS_OS_WINDOWS ? ">" : "→");
    public static final String REDIVID = (SystemUtils.IS_OS_WINDOWS ? "<" : "←");
    public static final String LEFT_PADDING = "  ";
    
    public static final String LAST_TREE_NODE = (SystemUtils.IS_OS_WINDOWS ? "\\- " : "└── ");
    public static final String LAST_TREE_NODE_WITH_CHILDREN = (SystemUtils.IS_OS_WINDOWS ? "\\- " : "└─┬ ");
    public static final String TREE_NODE = (SystemUtils.IS_OS_WINDOWS ? "+- " : "├── ");
    public static final String TREE_NODE_WITH_CHILDREN = (SystemUtils.IS_OS_WINDOWS ? "+- " : "├─┬ ");
    public static final String TREE_CONNECTOR = (SystemUtils.IS_OS_WINDOWS ? "|  " : "| ");
    
    public static final String CLOSEST_MATCH_HINT = "Did you mean?";
    
    public static String cliClient() {
        return ARTIFACT + " " + VersionUtils.readVersion().orElse("0.0.0");
    }

}
