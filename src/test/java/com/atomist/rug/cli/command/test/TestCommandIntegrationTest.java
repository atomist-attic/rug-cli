package com.atomist.rug.cli.command.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class TestCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testSuccessfulTests() throws Exception {
        assertCommandLine(0, () -> {
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-project-templates:common-editors:3.2.2"));
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("Successfully executed 13 of 13 scenarios: Test SUCCESS"));
        }, "test");
    }

    @Test
    public void testSuccessfulTestsForFile() throws Exception {
        assertCommandLine(0, () -> {
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-project-templates:common-editors:3.2.2"));
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("Successfully executed 2 of 2 scenarios: Test SUCCESS"));
        }, "test", "AddGitIgnore");
    }

    @Test
    public void testSuccessfulTestsWithResolverInformation() throws Exception {
        // Make sure the manifest newer than the cached resolver result
        Runtime.getRuntime().exec("touch .atomist/manifest.yml");
        assertCommandLine(0, () -> {
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("com.atomist:rug-cli-root:jar:1.0.0"));
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("Successfully executed 13 of 13 scenarios: Test SUCCESS"));
        }, "test", "-r");
    }

    @Test
    public void testUnSuccessfulForNonExistingTest() throws Exception {
        assertFailure("Specified test scenario or test file bla could not be found", "test", "bla");
    }
}
