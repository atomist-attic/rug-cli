package com.atomist.rug.cli.command.generate;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

/**
 * Separate to manage the output
 */
public class GenerateDotCommandIntegrationTest extends AbstractCommandTest {

    @Before
    public void before() throws Exception {

        String tmp = System.getProperty("java.io.tmpdir") + File.separator + "generateTest";
        File output = new File(tmp);
        output.mkdirs();
        IOUtils.copy(new FileInputStream(new File("src/test/resources/cli.yml")),
                new FileOutputStream(new File(System.getProperty("java.io.tmpdir"), "cli.yml")));

        setCWD(tmp);
    }

    @Test
    public void testSuccessfulGenerate() throws Exception {
        String id = System.currentTimeMillis() + "";
        testGenerationAt(id, ".");
    }

    void testGenerationAt(String name, String location) throws Exception {
        assertCommandLine(0, () -> {

            String absLocation = getCWD() + File.separator + location;
            File root = new File(absLocation, name);
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("Successfully generated new project " + name));
            assertTrue(new File(root, "src/main/java/my/test/HomeController.java").exists());
            assertTrue(new File(root, ".atomist.yml").exists());
            FileUtils.deleteQuietly(root);
        }, "generate", "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", name,
                "root_package=my.test", (!location.equals(".") ? "-C" : null),
                (!location.equals(".") ? location : null));
    }
}
