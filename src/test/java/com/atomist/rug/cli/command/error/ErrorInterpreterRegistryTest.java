package com.atomist.rug.cli.command.error;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ErrorInterpreterRegistryTest {

    private ErrorInterpreterRegistry registry = new ErrorInterpreterRegistry();
    
    @Test
    public void testOrdering() {
        assertEquals("TESTING", registry.interpret(new StringException("testing")));
        assertEquals("This is a test", registry.interpret(new StringException("This is a test")));
    }
    
    public static class TestErrorInterpreter implements ErrorInterpreter {

        @Override
        public boolean supports(String e) {
            return e.equals("testing");
        }

        @Override
        public int order() {
            return Integer.MIN_VALUE;
        }

        @Override
        public String interpret(String e) {
            if ("testing".equals(e)) {
                return "TESTING";
            }
            else {
                return "xxxxxxx";
            }
        }
    }
    
    @SuppressWarnings("serial")
    private static class StringException extends Exception {
        
        public StringException(String msg) {
            super(msg);
        }
    }

}
