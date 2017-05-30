package com.atomist.rug.cli.command.error;

/**
 * {@link ErrorInterpreter} that handles TypeScript compiler errors. 
 */
public class TypeScriptCompilerErrorInterpreter implements ErrorInterpreter {

    protected static final String ERROR_EXPLANATION = 
            "\nIt looks as if you are missing required node modules.\n\nPlease make sure to run 'yarn install' "
            + "or 'npm install' from within the .atomist folder to install all node module dependencies."
            + "\nIf this error persists, check that your 'package.json' lists all required dependencies.";

    @Override
    public boolean supports(String msg) {
        return msg.contains("TS2307: Cannot find module '@atomist");
    }

    @Override
    public String interpret(String msg) {
        return msg + ERROR_EXPLANATION;
    }

}
