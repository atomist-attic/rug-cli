package com.atomist.rug.cli.command.path;

import com.atomist.rug.cli.AbstractCommandTest;

import org.junit.Test;

public class PathCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testSingleJava() throws Exception {
        assertSuccess("Match (1 found)", "path",
                "/src/main/java/com/atomist/springrest/File()[@name='SpringRestApplication.java']/JavaType()",
                "--values");
    }

    @Test
    public void testMultipleJava() throws Exception {
        assertSuccess("Matches (2 found)", "path",
                "/src/main/java/com/atomist/springrest/*/JavaType()");
    }

    @Test
    public void testFailedParsing() throws Exception {
        assertFailure("", "path", "/src/main/java/com/atomist/springrest/");
    }
}
