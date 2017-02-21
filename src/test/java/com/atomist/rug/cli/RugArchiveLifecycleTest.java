package com.atomist.rug.cli;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RugArchiveLifecycleTest extends AbstractCommandTest {

    private static final String location = System.getProperty("java.io.tmpdir") + File.separator
            + System.currentTimeMillis();

    @Test
    public void testAGenerate() throws Exception {
        assertSuccess("Successfully generated new project my-rug-archive", "generate",
                "atomist-rugs:rug-archive:NewRugArchiveProject", "my-rug-archive",
                "group_id=atomist.rugs", "version=0.1.0",
                "description=My first Rug Archive project", "-C", location);
    }

    @Test
    public void testBEdit() throws Exception {
        assertSuccess("Successfully edited project my-rug-archive", "edit",
                "atomist-rugs:rug-archive:RemoveHelperFiles", "-C",
                location + File.separator + "my-rug-archive");
    }

    @Test
    public void testCDescribe() throws Exception {
        assertSuccess("To get more information on any of the Rugs listed above, run", "describe",
                "archive", "-l");
    }

    @Test
    public void testDTest() throws Exception {
        assertSuccess("Successfully executed 13 of 13 scenarios: Test SUCCESS", "test");
    }

    @Test
    public void testEInstall() throws Exception {
        assertSuccess("Successfully installed archive for rug-cli-tests:common-editors:3.2.2",
                "install");
    }

    @Test
    public void testFPublish() throws Exception {
        assertSuccess("Successfully published archive for rug-cli-tests:common-editors:3.2.2",
                "publish");
    }

    @AfterClass
    public static void cleanUp() {
        FileUtils.deleteQuietly(new File(location));
    }
}
