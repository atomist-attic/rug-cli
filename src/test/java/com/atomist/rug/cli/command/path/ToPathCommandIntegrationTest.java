package com.atomist.rug.cli.command.path;

import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class ToPathCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testJava() throws Exception {
        assertSuccess(
                "/File()[@path='com/atomist/rug/cli/Main.java']/JavaFile()",
                "to", "path", "com/atomist/rug/cli/Main.java", "--line", "34", "--column", "21",
                "--kind", "JavaFile", "--change-dir", "../../../main/java");
    }

    @Test
    public void testKindFailure() throws Exception {
        assertFailure("ScalaFile", "to", "path", "com/atomist/rug/cli/Main.java", "--line", "34",
                "--column", "21", "--kind", "TestKind", "--change-dir", "../../../main/java");
    }

    @Test
    public void testValidationFailure() throws Exception {
        assertFailure("Options --kind, --line and --column are required.", "to", "path",
                "com/atomist/rug/cli/Main.java", "--change-dir", "../../../main/java");
    }

}
