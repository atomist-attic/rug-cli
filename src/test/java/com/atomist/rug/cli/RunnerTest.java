package com.atomist.rug.cli;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.atomist.source.ArtifactSource;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;

public class RunnerTest extends AbstractCommandTest {

    @Test
    public void testDescribeHelp() throws Exception {
        assertSuccess("Usage: rug describe [OPTION]... TYPE ARTIFACT", "describe", "-?");
    }

    @Test
    public void testEditHelp() throws Exception {
        assertSuccess("Usage: rug edit [OPTION]... EDITOR", "edit", "-?");
    }

    @Test
    public void testGenerateHelp() throws Exception {
        assertSuccess("Usage: rug generate [OPTION]... GENERATOR", "generate", "-h");
    }

    @Test
    public void testHelp() throws Exception {
        assertSuccess("Usage: rug [OPTION]... [COMMAND]...", "-?");
    }

    @Test
    public void testHelpWithH() throws Exception {
        assertSuccess("Usage: rug [OPTION]... [COMMAND]...", "-h");
    }

    @Test
    public void testHelpWithHelp() throws Exception {
        assertSuccess("Usage: rug [OPTION]... [COMMAND]...", "--help");
    }

    @Test
    public void testInstallHelp() throws Exception {
        assertSuccess("Usage: rug install", "install", "--help");
    }

    @Test
    public void testInvalidOption() throws Exception {
        assertFailure("Run the following command for usage help:\n" + "  rug --help.", "-p");
    }

    @Test
    public void testPublishHelp() throws Exception {
        assertSuccess("Usage: rug publish", "publish", "--help");
    }

    @Test
    public void testTestHelp() throws Exception {
        assertSuccess("Usage: rug test [OPTION]... [TEST]", "test", "-h");
    }

    @Test
    public void testInvalidCommand() throws Exception {
        assertCommandLine(1, new SystemOutAssertion("Did you mean?\n" + "  rug generate"), true,
                "generator", "atomist-project-templates:spring-rest-service:SpringBoot",
                "testprojectname");
    }

    @Test
    public void testInvalidCommandWithOption() throws Exception {
        assertCommandLine(1, new SystemOutAssertion("Did you mean?\n" + "  rug generate"), true,
                "generator", "--help");
    }
}
