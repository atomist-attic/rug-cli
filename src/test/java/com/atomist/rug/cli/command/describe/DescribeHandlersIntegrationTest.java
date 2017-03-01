package com.atomist.rug.cli.command.describe;

import com.atomist.rug.cli.AbstractCommandTest;
import org.junit.Before;
import org.junit.Test;

public class DescribeHandlersIntegrationTest extends AbstractCommandTest {

    @Before
    public void before() throws Exception {
        setRelativeCWD("src/test/resources/handlers");
    }

    @Test
    public void testSuccessfulArchiveDescribe() throws Exception {
        assertSuccess(
                "→ Command Handlers\n" + 
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
                "describe", "-l", "archive");
    }

    @Test
    public void testSuccessfulCommandHandlerDescribe() throws Exception {
        assertSuccess(
                "→ Command Handlers\n" + 
                "  CreateIssue\n" + 
                "    Creates a GitHub issue\n" + 
                "  LicenseAdder\n" + 
                "    Runs the SetLicense editor on a bunch of my repos\n" + 
                "  ListIssuesHandler\n" + 
                "    Lists open github issues in slack",
                "describe", "-l", "command-handler");
    }

    @Test
    public void testSuccessfulSpecificCommandHandlerDescribe() throws Exception {
        assertSuccess(
                "ListIssuesHandler\n" + "rug-cli-tests:handlers:0.12.0\n"
                        + "Lists open github issues in slack\n" + "\n" + "→ Intent\n"
                        + "  list issues\n" + "→ Tags\n" + "  github (github)\n"
                        + "  issues (issues)\n" + "→ Parameters (optional)\n" + "  days \n"
                        + "    Days\n" + "    max: 100  default: 1  pattern: ^.*$\n" + "\n"
                        + "To invoke the ListIssuesHandler command-handler, run:\n"
                        + "  rug command \"rug-cli-tests:handlers:ListIssuesHandler\" -a 0.12.0 -l days=VALUE",
                "describe", "-l", "command-handler", "ListIssuesHandler");
    }
    
    @Test
    public void testSuccessfulSpecificCommandHandlerDescribeWithSecretsAndMappedParams() throws Exception {
        assertSuccess(
                "CreateIssue\n" + 
                "rug-cli-tests:handlers:0.12.0\n" + 
                "Creates a GitHub issue\n" + 
                "\n" + 
                "→ Intent\n" + 
                "  create issue\n" + 
                "→ Secrets\n" + 
                "  github/user_token=repo\n" + 
                "→ Mapped Parameters\n" + 
                "  repo => atomist/repository\n" + 
                "  owner => atomist/owner\n" + 
                "→ Tags\n" + 
                "  github (github)\n" + 
                "  issue (issue)\n" + 
                "→ Parameters (required)\n" + 
                "  title \n" + 
                "    Title of issue\n" + 
                "    pattern: ^.*$\n" + 
                "  body \n" + 
                "    Body of the issue\n" + 
                "    pattern: ^[\\s\\S]*$",
                "describe", "-l", "command-handler", "CreateIssue");
    }


    @Test
    public void testSuccessfulResponseHandlerDescribe() throws Exception {
        assertSuccess("→ Response Handlers\n" + 
                "  CreateIssue\n" + 
                "    Prints out the response message\n" + 
                "  IssueClosedResponder\n" + 
                "    Logs failed issue reopen attempts\n" + 
                "  Kitties\n" + 
                "    Prints out kitty urls", "describe", "-l", "response-handler");
    }

    @Test
    public void testSuccessfulSpecificResponseHandlerDescribe() throws Exception {
        assertSuccess(
                "Kitties\n" + "rug-cli-tests:handlers:0.12.0\n" + "Prints out kitty urls\n" + "\n"
                        + "→ Parameters\n" + "  no parameters needed\n" + "\n"
                        + "To invoke the Kitties response-handler, run:\n"
                        + "  rug respond \"rug-cli-tests:handlers:Kitties\" -a 0.12.0 -l",
                "describe", "-l", "response-handler", "Kitties");
    }

    @Test
    public void testSuccessfulEventHandlerDescribe() throws Exception {
        assertSuccess("Event Handlers\n" + "  ClosedIssueReopener\n" + "    Reopens closed issues",
                "describe", "-l", "event-handler");
    }

    @Test
    public void testSuccessfulSpecificEventHandlerDescribe() throws Exception {
        assertSuccess(
                "ClosedIssueReopener\n" + "rug-cli-tests:handlers:0.12.0\n"
                        + "Reopens closed issues\n" + "\n" + "→ Root node\n" + "  issue\n"
                        + "→ Tags\n" + "  github (github)\n" + "  issues (issues)\n" + "\n"
                        + "To invoke the ClosedIssueReopener event-handler, run:\n"
                        + "  rug trigger \"rug-cli-tests:handlers:ClosedIssueReopener\" -a 0.12.0 -l",
                "describe", "-l", "event-handler", "ClosedIssueReopener");
    }
}
