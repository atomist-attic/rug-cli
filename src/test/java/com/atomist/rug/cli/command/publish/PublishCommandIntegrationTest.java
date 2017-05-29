package com.atomist.rug.cli.command.publish;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class PublishCommandIntegrationTest extends AbstractCommandTest {

    @Before
    public void cleanUp() throws IOException {
        FileUtils.deleteDirectory(new File(getRepoDir(), "rug-cli-tests"));
    }

    @Test
    public void testSuccessfulPublish() throws Exception {
        assertCommandLine(0, () -> {
            assertVersion("rug-cli-tests", "common-editors", "0.8.0");
        }, "publish", "-F");
    }

    @Test
    public void testSuccessfulInstallWithGroupArtifactAndVersion() throws Exception {
        assertCommandLine(0, () -> {
            assertVersion("test-group", "test-artifact", "4.0.0");
        }, "publish", "--archive-group", "test-group", "--archive-artifact", "test-artifact", "-a",
                "4.0.0", "-F");
    }

    private void assertVersion(String group, String artifact, String version) throws IOException {
        assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                .contains(group + ":" + artifact + " (" + version + ")"));
        assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                .contains("Successfully published archive for " + group + ":" + artifact + " ("
                        + version + ")"));
        assertTrue(new File(getRepoDir(), group + File.separator + artifact + File.separator
                + version + File.separator + artifact + "-" + version + ".zip").exists());
        assertTrue(new File(getRepoDir(), group + File.separator + artifact + File.separator
                + version + File.separator + artifact + "-" + version + ".pom").exists());

    }

    private static File getRepoDir() throws IOException {
        File file = new File(SystemUtils.getUserDir() + File.separator + ".." + File.separator
                + ".." + File.separator + ".." + File.separator + ".." + File.separator + "target"
                + File.separator + "repository-publish").getCanonicalFile();
        return file;
    }
}
