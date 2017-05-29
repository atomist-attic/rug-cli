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
                "atomist-rugs:rug-editors:NewRugProject", "my-rug-archive", "owner=atomist-rugs",
                "description=blabla", "-Xur", "-C", location);
    }

    @Test
    public void testBEdit() throws Exception {
        assertSuccess("Successfully edited project my-rug-archive", "edit",
                "atomist-rugs:rug-editors:AddTypeScript", "-C", location + File.separator + "my-rug-archive");
    }
    
//    @Test
//    public void testCEdit() throws Exception {
//        assertSuccess("Successfully edited project my-rug-archive", "edit",
//                "atomist-rugs:rug-editors:AddTypeScriptEditor", "editor_name=MyEditor",
//                "description=My test", "-C", location + File.separator + "my-rug-archive");
//    }

    @Test
    public void testDDescribe() throws Exception {
        setCWD(location + File.separator + "my-rug-archive");
        assertSuccess("To get more information on any of the Rugs listed above, run", "describe",
                "archive", "-l");
    }

    // @Test
    // public void testDTest() throws Exception {
    // assertSuccess("Successfully executed 13 of 13 scenarios: Test SUCCESS", "test");
    // }

    @Test
    public void testFInstall() throws Exception {
        setCWD(location + File.separator + "my-rug-archive");
        assertSuccess("Successfully installed archive for atomist-rugs:my-rug-archive (0.1.0)",
                "install");
    }

    @Test
    public void testGPublish() throws Exception {
        setCWD(location + File.separator + "my-rug-archive");
        assertSuccess("Successfully published archive for atomist-rugs:my-rug-archive (0.1.0)",
                "publish");
    }

    @AfterClass
    public static void cleanUp() {
        FileUtils.deleteQuietly(new File(location));
    }
}
