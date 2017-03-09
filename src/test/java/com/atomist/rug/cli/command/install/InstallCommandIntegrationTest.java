package com.atomist.rug.cli.command.install;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class InstallCommandIntegrationTest extends AbstractCommandTest {

    @Before
    public void cleanUp() throws IOException {
        FileUtils.deleteDirectory(new File(FileUtils.getUserDirectory(),
                ".atomist" + File.separator + "repository" + File.separator + "rug-cli-tests"));
    }

    @Test
    public void testSuccessfulInstall() throws Exception {
        assertCommandLine(0, () -> {
            assertVersion("0.8.0");
        }, "install", "-Vur");
    }

    @Test
    public void testSuccessfulInstallWithVersion() throws Exception {
        assertCommandLine(0, () -> {
            assertVersion("4.0.0");
        }, "install", "-a", "4.0.0", "-Vur");
    }

    private void assertVersion(String version) {
        assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                .contains("rug-cli-tests:common-editors (" + version + ")"));
        assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                .contains("Successfully installed archive for rug-cli-tests:common-editors ("
                        + version + ")"));
        assertTrue(new File(FileUtils.getUserDirectory(),
                ".atomist" + File.separator + "repository-tests" + File.separator + "rug-cli-tests"
                        + File.separator + "common-editors" + File.separator + version
                        + File.separator + "common-editors-" + version + ".zip").exists());
        assertTrue(new File(FileUtils.getUserDirectory(),
                ".atomist" + File.separator + "repository-tests" + File.separator + "rug-cli-tests"
                        + File.separator + "common-editors" + File.separator + version
                        + File.separator + "common-editors-" + version + ".pom").exists());
        assertTrue(new File(FileUtils.getUserDirectory(),
                ".atomist" + File.separator + "repository-tests" + File.separator + "rug-cli-tests"
                        + File.separator + "common-editors" + File.separator + version
                        + File.separator + "common-editors-" + version + "-metadata.json")
                                .exists());

    }
}
