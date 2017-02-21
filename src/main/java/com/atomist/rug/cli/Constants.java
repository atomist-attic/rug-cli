package com.atomist.rug.cli;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang3.SystemUtils;

import com.atomist.rug.cli.version.VersionUtils;

public class Constants {

    private static final String COMMAND = "rug ";

    public static final String GROUP = "com.atomist";
    public static final String ARTIFACT = "rug-cli";

    public static final String RUG_ARTIFACT = "rug";
    public static final String RUG_VERSION_RANGE = "[0.12.0,1.0.0)";

    public static final String ATOMIST_ROOT = ".atomist";
    public static final String CLI_CONFIG_NAME = "cli.yml";

    public static final String DIVIDER = (SystemUtils.IS_OS_WINDOWS ? ">" : "→");
    public static final String REDIVID = (SystemUtils.IS_OS_WINDOWS ? "<" : "←");
    public static final String LEFT_PADDING = "  ";

    public static final int WRAP_LENGTH = 80;

    public static final String LAST_TREE_NODE = (SystemUtils.IS_OS_WINDOWS ? "\\- " : "└── ");
    public static final String LAST_TREE_NODE_WITH_CHILDREN = (SystemUtils.IS_OS_WINDOWS ? "\\- "
            : "└─┬ ");
    public static final String TREE_NODE = (SystemUtils.IS_OS_WINDOWS ? "+- " : "├── ");
    public static final String TREE_NODE_WITH_CHILDREN = (SystemUtils.IS_OS_WINDOWS ? "+- "
            : "├─┬ ");
    public static final String TREE_CONNECTOR = (SystemUtils.IS_OS_WINDOWS ? "|  " : "| ");

    public static final String CLOSEST_MATCH_HINT = "Did you mean?";

    public static final String CATALOG_PATH = "operation/search";
    public static final String CATALOG_URL = "https://api.atomist.com/catalog";
    public static final String REPO_URL = "https://api-staging.atomist.services/user/team";

    public static String cliClient() {
        return ARTIFACT + " " + VersionUtils.readVersion().orElse("0.0.0");
    }

    public static String httpClient() {
        return ARTIFACT + "-" + VersionUtils.readVersion().orElse("0.0.0");
    }

    public static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            return "unkown";
        }
    }

    // We need to persist this setting in the System properties as we are dealing with different
    // class loaders throughout the app and can't use static fields
    public static Boolean isShell() {
        return Boolean.valueOf(System.getProperty("RUG_CLI_SHELL", "false"));
    }

    public static void setShell(Boolean isShell) {
        System.setProperty("RUG_CLI_SHELL", isShell.toString());
    }

    public static String command() {
        // When running inside the shell we don't need command to show in help and error msg
        if (isShell()) {
            return "";
        }
        else {
            return COMMAND;
        }
    }
}
