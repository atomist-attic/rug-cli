package com.atomist.rug.cli.output;

import org.apache.commons.lang3.SystemUtils;

import com.github.tomaslanger.chalk.Chalk;

public class Style {

    public static String blue(String msg, Object... tokens) {
        if (Chalk.isColorEnabled()) {
            return Chalk.on(format(msg, tokens)).blue().toString();
        }
        return format(msg, tokens);
    }

    public static String bold(String msg, Object... tokens) {
        if (Chalk.isColorEnabled()) {
            return Chalk.on(format(msg, tokens)).bold().toString();
        }
        return format(msg, tokens);
    }

    public static String cyan(String msg, Object... tokens) {
        if (Chalk.isColorEnabled()) {
            return Chalk.on(format(msg, tokens)).cyan().toString();
        }
        return format(msg, tokens);
    }

    public static String green(String msg, Object... tokens) {
        if (Chalk.isColorEnabled()) {
            return Chalk.on(format(msg, tokens)).green().toString();
        }
        return format(msg, tokens);
    }

    public static String red(String msg, Object... tokens) {
        if (Chalk.isColorEnabled()) {
            return Chalk.on(format(msg, tokens)).red().toString();
        }
        return format(msg, tokens);
    }

    public static String underline(String msg, Object... tokens) {
        if (Chalk.isColorEnabled()) {
            return Chalk.on(format(msg, tokens)).underline().toString();
        }
        return format(msg, tokens);
    }

    public static String yellow(String msg, Object... tokens) {
        if (Chalk.isColorEnabled()) {
            return Chalk.on(format(msg, tokens)).yellow().toString();
        }
        return format(msg, tokens);
    }
    
    /**
     * Careful: gray doesn't work on Windows!
     */
    public static String gray(String msg, Object... tokens) {
        if (Chalk.isColorEnabled() && !SystemUtils.IS_OS_WINDOWS) {
            return Chalk.on(format(msg, tokens)).gray().toString();
        }
        return format(msg, tokens);
    }

    private static String format(String msg, Object... tokens) {
        if (tokens == null || tokens.length == 0) {
            return msg;
        }
        else {
            return String.format(msg, tokens);
        }
    }
}
