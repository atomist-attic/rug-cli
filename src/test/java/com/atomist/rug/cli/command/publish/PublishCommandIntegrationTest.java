package com.atomist.rug.cli.command.publish;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class PublishCommandIntegrationTest extends AbstractCommandTest {

	@Before
	public void cleanUp() throws IOException {
		FileUtils.deleteDirectory(new File(FileUtils.getUserDirectory(),
				".atomist" + File.separator + "repository-publish" + File.separator + "rug-cli-tests"));
	}

	@AfterClass
	public static void deleteRepo() throws IOException {
		FileUtils.deleteDirectory(
				new File(FileUtils.getUserDirectory(), ".atomist" + File.separator + "repository-publish"));
	}

	@Test
	public void testSuccessfulPublish() throws Exception {
		assertCommandLine(0, () -> {
			assertVersion("3.2.2");
		}, "publish");
	}

	@Test
	public void testSuccessfulInstallWithVersion() throws Exception {
		assertCommandLine(0, () -> {
			assertVersion("4.0.0");
		}, "publish", "-a", "4.0.0");
	}

	private void assertVersion(String version) {
		assertTrue(
				systemOutRule.getLogWithNormalizedLineSeparator().contains("rug-cli-tests:common-editors:" + version));
		assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
				.contains("Successfully published archive for rug-cli-tests:common-editors:" + version));
		assertTrue(new File(FileUtils.getUserDirectory(),
				".atomist" + File.separator + "repository-publish" + File.separator + "rug-cli-tests" + File.separator
						+ "common-editors" + File.separator + version + File.separator + "common-editors-" + version
						+ ".zip").exists());
		assertTrue(new File(FileUtils.getUserDirectory(),
				".atomist" + File.separator + "repository-publish" + File.separator + "rug-cli-tests" + File.separator
						+ "common-editors" + File.separator + version + File.separator + "common-editors-" + version
						+ ".pom").exists());

	}
}
