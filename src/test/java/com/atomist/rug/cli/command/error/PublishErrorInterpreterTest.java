package com.atomist.rug.cli.command.error;

import static org.junit.Assert.*;

import org.junit.Test;

public class PublishErrorInterpreterTest {

    private static final String ERROR_MESSAGE = 
            "Failed to deploy artifacts: Could not transfer artifact atomist:spring-team-handlers:zip:0.1.125 from/to t5964n9b7 (https://atomist.jfrog.io/atomist/T5964N9B7): Forbidden (403)";

    private ErrorInterpreter interpreter = new PublishErrorInterpreter();

    @Test
    public void testSupported() {
        assertTrue(interpreter.supports(ERROR_MESSAGE));
        assertTrue(interpreter.interpret(ERROR_MESSAGE).contains(
                "Please verify that the credentials for repository t5964n9b7 in cli.yml are valid."));

    }
}
