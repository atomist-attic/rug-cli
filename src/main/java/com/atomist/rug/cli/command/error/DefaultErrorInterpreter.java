package com.atomist.rug.cli.command.error;

/**
 * {@link ErrorInterpreter} that just returns the provided string without additional modification.
 * This implementation should be the last {@link ErrorInterpreter} in the chain. 
 */
public class DefaultErrorInterpreter implements ErrorInterpreter {

    @Override
    public boolean supports(String e) {
        return true;
    }

    @Override
    public String interpret(String e) {
        return e;
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }

}
