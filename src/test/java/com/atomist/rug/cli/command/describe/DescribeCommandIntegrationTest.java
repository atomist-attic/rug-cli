package com.atomist.rug.cli.command.describe;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;

import com.atomist.rug.cli.AbstractCommandTest;
import com.atomist.rug.cli.Constants;

public class DescribeCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testHelp() throws Exception {
        assertSuccess("Usage: rug describe [OPTION]... TYPE ARTIFACT", "describe", "editor", "-h");
    }

    @Test
    public void testInvalidOption() throws Exception {
        assertFailure("-j is not a valid option", "describe", "editor",
                "atomist-rugs:spring-boot-rest-service:AddLoCKback", "-j");
    }

    @Test
    public void testSuccessfulEditorDescribe() throws Exception {
        assertSuccess("rug edit \"rug-cli-tests:common-editors:AddChangeLog\"", "describe", "editor",
                "atomist-rugs:common-editors:AddChangeLog", "-l");
    }

    @Test
    public void testSuccessfulEditorDescribeWithNumberInName() throws Exception {
        assertSuccess("rug edit \"rug-cli-tests:common-editors:AddApacheSoftwareLicense20\"",
                "describe", "editor", "atomist-rugs:common-editors:AddApacheSoftwareLicense20", "-l");
    }

    @Test
    public void testSuccessfulGeneratorDescribe() throws Exception {
        assertSuccess(
                "rug generate \"atomist-rugs:spring-boot-rest-service:NewSpringBootRestService\"",
                "describe", "generator",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService");
    }

    @Test
    public void testSuccessfulArchiveDescribe() throws Exception {
        assertSuccess(
                "rug describe editor|generator|command-handler|event-handler|response-handler|function atomist-rugs:spring-boot-rest-service:NAME",
                "describe", "archive", "atomist-rugs:spring-boot-rest-service");
    }

    @Test
    public void testSuccessfulArchiveDescribeJson() throws Exception {
        assertCommandLine(0, new Assertion() {

            @Override
            public void checkAssertion() throws Exception {
                String sysout = systemOutRule.getLogWithNormalizedLineSeparator();
                String stderr = systemErrRule.getLogWithNormalizedLineSeparator();
                assertTrue(stderr.contains("Loading"));
                assertTrue(sysout.startsWith("{"));
            }
        }, "describe", "archive", "atomist-rugs:spring-boot-rest-service", "--output", "json");
    }

    @Test
    public void testSuccessfulArchiveDescribeYaml() throws Exception {
        assertCommandLine(0, new Assertion() {

            @Override
            public void checkAssertion() throws Exception {
                String sysout = systemOutRule.getLogWithNormalizedLineSeparator();
                String stderr = systemErrRule.getLogWithNormalizedLineSeparator();
                assertTrue(stderr.contains("Loading"));
                assertTrue(sysout.startsWith("---"));
            }
        }, "describe", "archive", "atomist-rugs:spring-boot-rest-service", "--output", "yaml");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersion() throws Exception {
        assertSuccess("atomist-rugs:spring-boot-rest-service (0.10.0" + Constants.DOT + "zip)",
                "describe", "generator",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", "-a", "0.10.0");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersionAndResolverReport() throws Exception {
        // delete the resolver plan file
        File file = new File(System.getProperty("user.home"),
                ".atomist/repository-tests/atomist-rugs/spring-boot-rest-service/0.10.0/_resolver.plan");
        file.delete();
        assertSuccess("Binary dependency report for atomist-rugs:spring-boot-rest-service (0.10.0)",
                "describe", "generator",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", "-a", "0.10.0",
                "-r");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersionAndUpdate() throws Exception {
        assertSuccess("Binary dependency report for atomist-rugs:spring-boot-rest-service (0.10.0)",
                "describe", "generator",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", "-a", "0.10.0",
                "-ru");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersionOffline() throws Exception {
        assertCommandLine(0, () -> assertFalse(systemOutRule.getLog().contains("Downloading ")),
                "describe", "generator",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", "-o", "-a",
                "0.10.0");
    }

    @Test
    public void testSuccessfulLocalEditorDescribeWithNoKind() throws Exception {
        assertSuccess("AddChangeLog (excluded)", "describe", "AddChangeLog", "-l");
    }

    @Test
    public void testSuccessfulLocalEditorDescribe() throws Exception {
        assertSuccess("AddChangeLog (excluded)", "describe", "editor", "AddChangeLog", "-l");
    }

    @Test
    public void testUnSuccessfulDescribe() throws Exception {
        assertFailure(
                "Invalid TYPE provided. Please use either archive, editor, generator, command-handler, event-handler, response-handler, function or dependencies."
                        + "\n\n" + "Run the following command for usage help:\n"
                        + "  rug describe --help",
                "describe", "-l");
    }

    @Test
    public void testUnSuccessfulDescribeArchiveForgotType() throws Exception {
        String archiveIWantedToDescribe = "atomist-rugs:spring-boot-rest-service";
        assertFailure(
                "It looks like you're trying to describe an archive. Please try:\n"
                        + "  rug describe archive " + archiveIWantedToDescribe + "\n" + "\n"
                        + "Run the following command for usage help:\n" + "  rug describe --help",
                "describe", archiveIWantedToDescribe);
    }

    @Test
    public void testUnSuccessfulDescribeEditorForgotType() throws Exception {
        String editorIWantedToDescribe = "atomist-rugs:spring-boot-rest-service:NewBootyThing";
        assertFailure(
                "Please tell me what kind of thing to describe. Try:\n"
                        + "  rug describe editor|generator|event-handler|command-handler|response-handler|function "
                        + editorIWantedToDescribe + "\n" + "\n"
                        + "Run the following command for usage help:\n" + "  rug describe --help",
                "describe", editorIWantedToDescribe);
    }

    @Test
    public void testUnSuccessfulDescribeOffline() throws Exception {
        assertFailure(
                "No valid ARTIFACT provided, no default artifact defined and not in local mode.\n"
                        + "Please specify a valid artifact identifier or run with -l to load your local project.\n"
                        + "\n" + "Run the following command for usage help:\n"
                        + "  rug describe --help",
                "-oX", "describe");
    }

    @Test
    public void testUnSuccessfulEditorDescribe() throws Exception {
        assertFailure("Specified editor AddLoCKback could not be found", "describe", "editor",
                "atomist-rugs:spring-boot-rest-service:AddLoCKback");
    }

    @Test
    public void testUnSuccessfulGeneratorDescribe() throws Exception {
        assertFailure("Did you mean?\n" + "  NewSpringBootRestService", "describe", "generator",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestServi");
    }

    @Test
    public void testUnSuccessfulGeneratorDescribeForNonExistingArchive() throws Exception {
        assertFailure("Unable to resolve requested archive rug-cli-tests:i-do-not-exist (latest).",
                "describe", "generator", "rug-cli-tests:i-do-not-exist:SpringBootRestMicroservice");
    }
}
