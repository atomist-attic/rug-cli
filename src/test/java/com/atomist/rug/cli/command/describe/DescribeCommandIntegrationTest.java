package com.atomist.rug.cli.command.describe;

import com.atomist.rug.cli.AbstractCommandTest;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertSuccess("rug edit \"atomist-rugs:spring-boot-rest-service:SwitchReadmes\"",
                "describe", "editor", "atomist-rugs:spring-boot-rest-service:SwitchReadmes");
    }

    @Test
    public void testSuccessfulEditorDescribeWithNumberInName() throws Exception {
        assertSuccess("rug edit \"atomist-rugs:common-editors:AddApacheSoftwareLicense20\"",
                "describe", "editor", "atomist-rugs:common-editors:AddApacheSoftwareLicense20");
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
        assertSuccess("rug describe editor|generator|reviewer|command-handler|event-handler|response-handler ARTIFACT", "describe",
                "archive", "atomist-rugs:spring-boot-rest-service");
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
        }, "describe", "archive", "atomist-rugs:spring-boot-rest-service", "-O", "json");
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
        }, "describe", "archive", "atomist-rugs:spring-boot-rest-service", "-O", "yaml");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersion() throws Exception {
        assertSuccess("atomist-rugs:spring-boot-rest-service:0.5.0", "describe", "generator",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", "-a", "0.5.0");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersionAndResolverReport() throws Exception {
        // delete the resolver plan file
        File file = new File(System.getProperty("user.home"),
                ".atomist/repository-tests/atomist-rugs/spring-boot-rest-service/0.5.0/_resolver.plan");
        file.delete();
        assertSuccess("Dependency report for atomist-rugs:spring-boot-rest-service:0.5.0",
                "describe", "generator",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", "-a", "0.5.0",
                "-r");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersionAndUpdate() throws Exception {
        assertSuccess("Dependency report for atomist-rugs:spring-boot-rest-service:0.5.0",
                "describe", "generator",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", "-a", "0.5.0",
                "-ru");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersionOffline() throws Exception {
        assertCommandLine(0, () -> assertFalse(systemOutRule.getLog().contains("Downloading ")),
                "describe", "generator",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", "-o", "-a",
                "0.5.0");
    }

    @Test
    public void testSuccessfulLocalEditorDescribe() throws Exception {
        assertSuccess("rug edit \"rug-cli-tests:common-editors:AddGitIgnore\"", "describe",
                "editor", "rug-cli-tests.common-editors.AddGitIgnore", "-l");
    }

    @Test
    public void testUnSuccessfulDescribe() throws Exception {
        assertFailure(
                "com.atomist.rug.cli.command.CommandException: Invalid TYPE provided. Please tell me what you would like to describe: archive, editor, generator, reviewer, command-handler, event-handler, or response-handler \n"
                        + "\n" + "Run the following command for usage help:\n"
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
                        + "  rug describe editor|generator|reviewer|event-handler|command-handler|response-handler "
                        + editorIWantedToDescribe + "\n" + "\n"
                        + "Run the following command for usage help:\n" + "  rug describe --help",
                "describe", editorIWantedToDescribe);
    }

    @Test
    public void testUnSuccessfulDescribeOffline() throws Exception {
        assertFailure(
                "No valid ARTIFACT provided, no default artifact defined and not in local mode.\n\n"
                        + "Run the following command for usage help:\n" + "  rug describe --help",
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
        assertFailure("Unable to resolve requested archive rug-cli-tests:i-do-not-exist:latest.",
                "describe", "generator", "rug-cli-tests:i-do-not-exist:SpringBootRestMicroservice");
    }
}
