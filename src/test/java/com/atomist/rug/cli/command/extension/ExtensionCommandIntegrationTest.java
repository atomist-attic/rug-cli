package com.atomist.rug.cli.command.extension;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.atomist.rug.cli.AbstractCommandTest;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExtensionCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testASuccessfullInstall() throws Exception {
        assertSuccess("Successfully installed extension com.atomist:source-lib", "extension",
                "install", "com.atomist:source-lib", "-a", "0.2.9-20161030094410");
    }

    @Test
    public void testBSuccessfullList() throws Exception {
        assertSuccess("source-lib-0.2.9-20161030094410.jar", "extension", "list");
    }

    @Test
    public void testCSuccessfullUninstall() throws Exception {
        assertSuccess("Successfully uninstalled extension com.atomist:source-lib", "extension",
                "uninstall", "com.atomist:source-lib");
    }

    @Test
    public void testDSuccessfullListWithNoInstalledExtensions() throws Exception {
        assertSuccess("No extensions installed", "extension", "list");
    }

    @Test
    public void testUnSuccessfullInstall() throws Exception {
        assertFailure("No valid EXTENSION provided", "extension", "install");
    }

    @Test
    public void testUnSuccessfullUninstall() throws Exception {
        assertFailure("No valid EXTENSION provided", "extension", "uninstall");
    }

    @Test
    public void testUnSuccessfullUninstallWithInvalidArtifact() throws Exception {
        assertFailure("No valid EXTENSION provided", "extension", "uninstall", "foobar");
    }

    @Test
    public void testUnSuccessfullUninstallWithNotInstalledArtifact() throws Exception {
        assertFailure("Extension com.atomist:foobar is not installed", "extension", "uninstall",
                "com.atomist:foobar");
    }

    @Test
    public void testSuccessfullHelp() throws Exception {
        assertSuccess("Manage command line extensions", "extension", "-?");
    }

    @Test
    public void testUnSuccessfullWithMissingSubCommand() throws Exception {
        assertFailure("Not enough or invalid arguments provided", "extension", "bla");
    }
}
