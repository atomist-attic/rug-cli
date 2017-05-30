package com.atomist.rug.cli.command.error;

import static org.junit.Assert.*;

import org.junit.Test;

public class DefaultErrorInterpreterTest {
    
    private ErrorInterpreter interpreter = new DefaultErrorInterpreter();

    @Test
    public void testIdentityErrorMessage() {
        String msg = "This is just any random command exception";
        assertTrue(interpreter.supports(msg));
        assertEquals(msg, interpreter.interpret(msg));
    }
}
