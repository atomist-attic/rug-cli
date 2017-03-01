package com.atomist.rug.cli.command.test;

import com.atomist.rug.cli.AbstractCommandTest;

public class TestCommandIntegrationTest extends AbstractCommandTest {

    // @Test
    // public void testSuccessfulTests() throws Exception {
    // assertCommandLine(0, () -> {
    // assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
    // .contains("rug-cli-tests:common-editors:3.2.2"));
    // assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
    // .contains("Successfully executed 13 of 13 scenarios: Test SUCCESS"));
    // }, "test");
    // }
    //
    // @Test
    // public void testSuccessfulTestsForFile() throws Exception {
    // assertCommandLine(0, () -> {
    // assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
    // .contains("rug-cli-tests:common-editors:3.2.2"));
    // assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
    // .contains("Successfully executed 2 of 2 scenarios: Test SUCCESS"));
    // }, "test", "AddGitIgnore");
    // }
    //
    // @Test
    // public void testSuccessfulTestsWithResolverInformation() throws Exception {
    // assertCommandLine(0, () -> {
    // assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
    // .contains("com.atomist:rug-cli-root:jar:1.0.0"));
    // assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
    // .contains("Successfully executed 13 of 13 scenarios: Test SUCCESS"));
    // }, "test", "-ru");
    // }
    //
    // @Test
    // public void testUnSuccessfulForNonExistingTest() throws Exception {
    // assertFailure("Specified test scenario or test file bla could not be found", "test", "bla");
    // }
}
