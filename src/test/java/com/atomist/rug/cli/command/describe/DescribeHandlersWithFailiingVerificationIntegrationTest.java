package com.atomist.rug.cli.command.describe;

import org.junit.Before;
import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class DescribeHandlersWithFailiingVerificationIntegrationTest extends AbstractCommandTest {

    @Before
    public void before() throws Exception {
        setRelativeCWD("src/test/resources/handlers-failing-verification");
    }

    @Test
    public void testSuccessfulVerificationFailure() throws Exception {
        assertFailure("Verification of com.atomist.rug:rug-functions-github (0.14.1) failed",
                "describe", "-lur", "archive");
    }

    @Test
    public void testSuccessfulDisableVerificationFailure() throws Exception {
        assertSuccess("→ Command Handlers\n" + 
                "  CreateIssue\n" + 
                "    Creates a GitHub issue\n" + 
                "  LicenseAdder\n" + 
                "    Runs the SetLicense editor on a bunch of my repos\n" + 
                "  ListIssuesHandler\n" + 
                "    Lists open github issues in slack\n" + 
                "→ Event Handlers\n" + 
                "  ClosedIssueReopener\n" + 
                "    Reopens closed issues\n" + 
                "  SayThankYou\n" + 
                "    Send a thank you message to a slack channel after an issue was closed\n" + 
                "→ Response Handlers\n" + 
                "  CreateIssue\n" + 
                "    Prints out the response message\n" + 
                "  IssueClosedResponder\n" + 
                "    Logs failed issue reopen attempts\n" + 
                "  Kitties\n" + 
                "    Prints out kitty urls",
                "describe", "-lur", "archive", "--disable-extension-verification");
    }
}
