package com.atomist.rug.cli.command.describe;

import static org.junit.Assert.assertFalse;

import java.io.File;

import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class DescribeCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testHelp() throws Exception {
        assertSuccess("Usage: rug describe [OPTION]... TYPE ARTIFACT", "describe", "editor", "-h");
    }

    @Test
    public void testInvalidOption() throws Exception {
        assertFailure("-j is not a valid option", "describe", "editor",
                "atomist-project-templates:spring-rest-service:AddLoCKback", "-j");
    }

    @Test
    public void testSuccessfulEditorDescribe() throws Exception {
        assertSuccess("rug edit \"atomist-project-templates:spring-rest-service:SwitchReadmes\"",
                "describe", "editor",
                "atomist-project-templates:spring-rest-service:SwitchReadmes");
    }

    @Test
    public void testSuccessfulGeneratorDescribe() throws Exception {
        assertSuccess(
                "rug generate \"atomist-project-templates:spring-rest-service:Spring Boot Rest Microservice\"",
                "describe", "generator",
                "atomist-project-templates:spring-rest-service:Spring Boot Rest Microservice");
    }

    @Test
    public void testSuccessfulArchiveDescribe() throws Exception {
        assertSuccess("rug describe editor|generator|executor|reviewer ARTIFACT", "describe",
                "archive", "atomist-project-templates:spring-rest-service");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersion() throws Exception {
        assertSuccess("atomist-project-templates:spring-rest-service:3.5.2", "describe",
                "generator",
                "atomist-project-templates:spring-rest-service:Spring Boot Rest Microservice", "-a",
                "3.5.2");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersionAndResolverReport() throws Exception {
        // delete the resolver plan file
        File file = new File(System.getProperty("user.home"),
                ".atomist/repository/atomist-project-templates/spring-rest-service/3.5.2/_resolver.plan");
        file.delete();
        assertSuccess("Dependency report for atomist-project-templates:spring-rest-service:3.5.2",
                "describe", "generator",
                "atomist-project-templates:spring-rest-service:Spring Boot Rest Microservice", "-a",
                "3.5.2", "-r");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersionOffline() throws Exception {
        assertCommandLine(0, () -> assertFalse(systemOutRule.getLog().contains("Downloading ")), "describe", "generator",
                "atomist-project-templates:spring-rest-service:Spring Boot Rest Microservice", "-a",
                "3.5.2", "-o");
    }

    @Test
    public void testSuccessfulLocalEditorDescribe() throws Exception {
        assertSuccess("rug edit \"atomist-project-templates:common-editors:AddGitIgnore\"",
                "describe", "editor", "atomist-project-templates.common-editors.AddGitIgnore",
                "-l");
    }

    @Test
    public void testUnSuccessfulDescribe() throws Exception {
        assertFailure("No or invalid TYPE provided.\n"
                + "Run the following command for usage help:\n" + "  rug describe --help.", "-X",
                "describe", "-l");
    }

    @Test
    public void testUnSuccessfulDescribeOffline() throws Exception {
        assertFailure(
                "No valid ARTIFACT provided, no default artifact defined and not in local mode.\n"
                        + "Run the following command for usage help:\n" + "  rug describe --help.",
                "-oX", "describe");
    }

    @Test
    public void testUnSuccessfulEditorDescribe() throws Exception {
        assertFailure("Specified editor AddLoCKback could not be found", "describe", "editor",
                "atomist-project-templates:spring-rest-service:AddLoCKback");
    }

    @Test
    public void testUnSuccessfulGeneratorDescribe() throws Exception {
        assertFailure("Specified generator Spring Boot Rest Microservice1 could not be found",
                "describe", "generator",
                "atomist-project-templates:spring-rest-service:Spring Boot Rest Microservice1");
    }

    @Test
    public void testUnSuccessfulGeneratorDescribeForNonExistingArchive() throws Exception {
        assertFailure("No version found for atomist-project-templates:i-do-not-exist", "describe",
                "generator",
                "atomist-project-templates:i-do-not-exist:Spring Boot Rest Microservice");
    }
}
