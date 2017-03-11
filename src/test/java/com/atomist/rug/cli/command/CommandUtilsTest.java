package com.atomist.rug.cli.command;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CommandUtilsTest {

    @Test
    public void testDoubleQuote() {
        String[] args = CommandUtils.splitCommandline("rug init-rug-archive test=test1 desc=\"bla bla\"");
        assertEquals("desc=\"bla bla\"", args[3]);
    }

}
