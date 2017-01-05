package com.atomist.rug.cli.command;

import java.io.File;
import java.util.Optional;

import org.apache.commons.cli.Options;

import com.atomist.rug.cli.Constants;

public abstract class CommandUtils {

    public static File getRequiredWorkingDirectory() {
        Optional<File> projectDir = getWorkingDirectory(System.getProperty("user.dir"));
        return projectDir.orElseThrow(() -> new CommandException(
                "Current directory is not a valid archive directory. Couldn't find .atomist folder."));
    }

    public static Optional<File> getWorkingDirectory() {
        return getWorkingDirectory(System.getProperty("user.dir"));
    }

    public static void main(String[] args) {
        new ServiceLoadingCommandInfoRegistry().commands().forEach(c -> {
            System.out.println(c.name());
            System.out.println("");
            c.globalOptions().getOptions().forEach(o -> System.out
                    .println(String.format("  -%s,--%s", o.getOpt(), o.getLongOpt())));
            System.out.println("");
            c.options().getOptions().forEach(o -> System.out
                    .println(String.format("  -%s,--%s", o.getOpt(), o.getLongOpt())));
            System.out.println("");
        });
    }

    public static Options options() {
        Options options = new Options();
        options.addOption("v", "version", false, "Print version information");
        options.addOption("?", "help", false, "Print help information");
        options.addOption("h", "help", false, "Print help information");
        options.addOption("q", "quiet", false, "Do not display progress messages");
        return options;
    }

    private static Optional<File> getWorkingDirectory(String root) {
        if (root == null) {
            root = System.getProperty("user.dir");
        }
        File projectRoot = searchFromProjectRoot(new File(root));
        return Optional.ofNullable(projectRoot);
    }

    private static File searchFromProjectRoot(File root) {
        // inside project root with a child .atomist
        File dir = new File(root, Constants.ATOMIST_ROOT);
        if (dir.exists()) {
            return root;
        }
        // inside .atomist folder
        if (root.getName().equals(Constants.ATOMIST_ROOT)) {
            return root.getParentFile();
        }
        // inside any sub-folder of .atomist
        if (root.getParentFile().getName().equals(Constants.ATOMIST_ROOT)) {
            return root.getParentFile().getParentFile();
        }
        return null;
    }
}
