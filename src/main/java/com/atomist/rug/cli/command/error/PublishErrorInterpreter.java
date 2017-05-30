package com.atomist.rug.cli.command.error;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ErrorInterpreter} to handle publication failure messages.
 */
public class PublishErrorInterpreter implements ErrorInterpreter {

    private static final Pattern ERROR_MESSAGE = Pattern.compile(
            "^^Failed to deploy artifacts: Could not transfer artifact .* from\\/to (.*) \\(.*\\): Forbidden \\(403\\)$$");

    @Override
    public boolean supports(String e) {
        return ERROR_MESSAGE.matcher(e).matches();
    }

    @Override
    public String interpret(String e) {
        Matcher matcher = ERROR_MESSAGE.matcher(e);
        matcher.matches();
        return e + String.format(
                "\n\nIt looks as if you are trying to publish an archive without incrementing the version."
                + "\nPlease increment the version in the 'package.json' and run the publish command again."
                + "\n\nAlternatively publication could fail due to invalid or missing credentials."
                + "\nPlease verify that the credentials for repository %s in cli.yml are valid.",
                matcher.group(1));
    }

}
