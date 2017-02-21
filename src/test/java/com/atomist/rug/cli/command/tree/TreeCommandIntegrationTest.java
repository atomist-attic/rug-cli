package com.atomist.rug.cli.command.tree;

import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

import static org.junit.Assert.fail;

public class TreeCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testSingleJava() throws Exception {
        assertSuccess("Match (1 found)", "tree",
                "/src/main/java/com/atomist/springrest/File()[@name='SpringRestApplication.java']/JavaType()",
                "--values");
        fail();// Check output from above.
    }

    @Test
    public void testMultipleJava() throws Exception {
        assertSuccess("Matches (2 found)", "tree",
                "/src/main/java/com/atomist/springrest/*/JavaType()");
    }

    @Test
    public void testFailedParsing() throws Exception {
        assertFailure("", "tree", "/src/main/java/com/atomist/springrest/");
    }
}
