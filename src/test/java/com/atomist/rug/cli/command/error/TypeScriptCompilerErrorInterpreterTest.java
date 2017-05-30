package com.atomist.rug.cli.command.error;

import static org.junit.Assert.*;

import org.junit.Test;

public class TypeScriptCompilerErrorInterpreterTest {

    private static final String SUPPORTED_ERROR = ".atomist/handlers/command/EnableRepo.ts(19,8): error TS2307: Cannot find module '@atomist/rug/operations/Decorators'.\n"
            + "} from \"@atomist/rug/operations/Decorators\";\n" + "       ^\n"
            + ".atomist/handlers/command/EnableRepo.ts(22,8): error TS2307: Cannot find module '@atomist/rug/operations/Handlers'.\n"
            + "} from \"@atomist/rug/operations/Handlers\";\n" + "       ^\n"
            + ".atomist/handlers/command/EnableRepo.ts(24,22): error TS2307: Cannot find module '@atomist/rugs/operations/CommonHandlers'.\n"
            + "import { wrap } from \"@atomist/rugs/operations/CommonHandlers\";\n"
            + "                     ^\n"
            + ".atomist/handlers/command/RepoToggle.ts(19,8): error TS2307: Cannot find module '@atomist/rug/operations/Handlers'.\n"
            + "} from \"@atomist/rug/operations/Handlers\";\n" + "       ^\n"
            + ".atomist/handlers/command/RepoToggle.ts(21,22): error TS2307: Cannot find module '@atomist/rugs/operations/CommonHandlers'.\n"
            + "import { wrap } from \"@atomist/rugs/operations/CommonHandlers\"";

    private ErrorInterpreter interpreter = new TypeScriptCompilerErrorInterpreter();

    @Test
    public void testSupported() {
        assertTrue(interpreter.supports(SUPPORTED_ERROR));
        assertTrue(interpreter.interpret(SUPPORTED_ERROR)
                .endsWith(TypeScriptCompilerErrorInterpreter.ERROR_EXPLANATION));
    }

    @Test
    public void testUnSupported() {
        assertFalse(interpreter.supports("This is just any error"));
    }

}
