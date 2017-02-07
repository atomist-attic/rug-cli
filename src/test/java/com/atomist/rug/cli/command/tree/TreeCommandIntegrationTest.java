package com.atomist.rug.cli.command.tree;

import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class TreeCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testSingleJava() throws Exception {
        assertSuccess("Match (1 found)", "tree", "/src/main/java/com/atomist/springrest/File()[@name='SpringRestApplication.java']/JavaType()");
    }

    @Test
    public void testMultipleJava() throws Exception {
        assertSuccess("Match (2 found)", "tree", "/src/main/java/com/atomist/springrest/*/JavaType()");
    }

    @Test
    public void testFailedParsing() throws Exception {
        assertFailure("", "tree", "/src/main/java/com/atomist/springrest/");
    }
}
