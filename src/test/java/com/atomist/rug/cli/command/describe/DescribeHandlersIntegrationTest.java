package com.atomist.rug.cli.command.describe;

import com.atomist.rug.cli.AbstractCommandTest;
import org.junit.Before;
import org.junit.Test;

public class DescribeHandlersIntegrationTest extends AbstractCommandTest{
    @Before
    public void before() throws Exception{
        setRelativeCWD("src/test/resources/handlers");
    }

    @Test
    public void testSuccessfulCommandHandlerDescribe() throws Exception {
        printCWD();
        assertSuccess("Command Handlers\n" +
                        "  LicenseAdder\n" +
                        "    Runs the SetLicense editor on a bunch of my repos\n" +
                        "  ListIssuesHandler\n" +
                        "    Lists open github issues in slack",
                "describe", "-l", "command-handler");
    }

    @Test
    public void testSuccessfulSpecificCommandHandlerDescribe() throws Exception {
        printCWD();
        assertSuccess("ListIssuesHandler\n" +
                        "rug-cli-tests:handlers:0.12.0\n" +
                        "Lists open github issues in slack\n" +
                        "\n" +
                        "→ Intent\n" +
                        "  list issues\n" +
                        "→ Tags\n" +
                        "  github (github)\n" +
                        "  issues (issues)\n" +
                        "→ Parameters (optional)\n" +
                        "  days \n" +
                        "    Days\n" +
                        "    max: 100  default: 1  pattern: ^.*$\n" +
                        "\n" +
                        "To invoke the ListIssuesHandler command-handler, run:\n" +
                        "  rug command \"rug-cli-tests:handlers:ListIssuesHandler\" -a 0.12.0 -l days=VALUE",
                "describe", "-l", "command-handler", "ListIssuesHandler");
    }

    @Test
    public void testSuccessfulResponseHandlerDescribe() throws Exception {
        printCWD();
        assertSuccess("Response Handlers\n" +
                        "  IssueClosedResponder\n" +
                        "    Logs failed issue reopen attempts\n" +
                        "  Kitties\n" +
                        "    Prints out kitty urls",
                "describe", "-l", "response-handler");
    }

    @Test
    public void testSuccessfulSpecificResponseHandlerDescribe() throws Exception {
        printCWD();
        assertSuccess("Kitties\n" +
                        "rug-cli-tests:handlers:0.12.0\n" +
                        "Prints out kitty urls\n" +
                        "\n" +
                        "→ Parameters\n" +
                        "  no parameters needed\n" +
                        "\n" +
                        "To invoke the Kitties response-handler, run:\n" +
                        "  rug respond \"rug-cli-tests:handlers:Kitties\" -a 0.12.0 -l",
                "describe", "-l", "response-handler", "Kitties");
    }

    @Test
    public void testSuccessfulEventHandlerDescribe() throws Exception {
        printCWD();
        assertSuccess("Event Handlers\n" +
                        "  ClosedIssueReopener\n" +
                        "    Reopens closed issues",
                "describe", "-l", "event-handler");
    }


    @Test
    public void testSuccessfulSpecificEventHandlerDescribe() throws Exception {
        printCWD();
        assertSuccess("ClosedIssueReopener\n" +
                        "rug-cli-tests:handlers:0.12.0\n" +
                        "Reopens closed issues\n" +
                        "\n" +
                        "→ Root node\n" +
                        "  issue\n" +
                        "→ Tags\n" +
                        "  github (github)\n" +
                        "  issues (issues)\n" +
                        "\n" +
                        "To invoke the ClosedIssueReopener event-handler, run:\n" +
                        "  rug trigger \"rug-cli-tests:handlers:ClosedIssueReopener\" -a 0.12.0 -l",
                "describe", "-l", "event-handler", "ClosedIssueReopener");
    }
}
