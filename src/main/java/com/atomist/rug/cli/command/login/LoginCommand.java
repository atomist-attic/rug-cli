package com.atomist.rug.cli.command.login;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.login.LoginOperations.Status;
import com.atomist.rug.cli.command.shell.ShellUtils;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;

public class LoginCommand extends AbstractAnnotationBasedCommand {

    private static final String BANNER = "The command will create a GitHub Personal Access Token with scope 'read:org'\n"
            + "which you can revoke any time on https://github.com/settings/tokens.  Your\n"
            + "password will not be displayed or stored.  Your sensitive information will not\n"
            + "be sent to Atomist; only to api.github.com.";

    @Command
    public void run(@Option("username") String username, @Option("mfa-code") String code,
            Settings settings) {
        login(username, code, settings);
    }

    private void login(String username, String code, Settings settings) {
        printBanner();

        LineReader reader = ShellUtils.lineReader(null);
        try {
            if (username == null) {
                username = reader.readLine(getPrompt("Username"));
            }
            String password = reader.readLine(getPrompt("Password"), new Character('*'));
            postForTokenAndHandleResponse(username, password, code, settings, reader);
        }
        catch (EndOfFileException | UserInterruptException e) {
            log.error("Canceled!");
        }
        finally {
            ShellUtils.shutdown(reader);
        }
    }

    private void printBanner() {
        log.newline();
        log.info("The Rug CLI needs your GitHub login to identify you.");
        log.newline();

        String banner = BANNER.replace("https://github.com/settings/tokens",
                Style.underline("https://github.com/settings/tokens"));
        banner = banner.replace("sensitive information will not\nbe sent to Atomist",
                Style.bold("sensitive information will not\nbe sent to Atomist"));
        log.info(banner);

        log.newline();
    }

    private void postForTokenAndHandleResponse(String username, String password, String code,
            Settings settings, LineReader reader) {
        Status status = new LoginOperations().postForToken(username, password, code, settings);

        if (status == Status.OK) {
            log.newline();
            log.info(Style.green(
                    "Successfully logged in to GitHub and stored token in ~/.atomist/cli.yml"));
        }
        else if (status == Status.BAD_CREDENTIALS) {
            throw new CommandException(
                    "Provided credentials are invalid. Please try again with correct credentials.",
                    "login");
        }
        else if (status == Status.MFA_REQUIRED) {
            log.newline();
            log.info("  Please provide a MFA code");
            code = reader.readLine(getPrompt("MFA code"));
            postForTokenAndHandleResponse(username, password, code, settings, reader);
        }
    }

    private String getPrompt(String name) {
        return String.format("  %s %s : ", Style.cyan(Constants.DIVIDER), Style.yellow(name));
    }
}