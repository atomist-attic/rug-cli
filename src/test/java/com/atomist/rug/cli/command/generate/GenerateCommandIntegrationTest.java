package com.atomist.rug.cli.command.generate;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class GenerateCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testSuccessfulGenerateAtLocation() throws Exception {
        String id = System.currentTimeMillis() + "";
        testGenerationAt(id, System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void testSuccessfulGenerateWithVeryLongProjectName() throws Exception {
        assertSuccess(
                "Successfully generated new project 1234567891234567891212345678912345678912",
                "generate", "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService",
                "1234567891234567891212345678912345678912", "-urF");
    }

    @Test
    public void testUnSuccessfulGenerateWithMissingParameter() throws Exception {
        assertFailure("No PROJECT_NAME provided", "generate",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService");
    }

    @Test
    public void testUnSuccessfulGenerateWithInvalidName() throws Exception {
        assertFailure("Did you mean?\n" + "  NewSpringBootRestService", "generate",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestServi", "test");
    }

    void testGenerationAt(String name, String location) throws Exception {
        assertCommandLine(0, () -> {
            File root = new File(location, name);
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("Successfully generated new project " + name));
            assertTrue(new File(root, "src/main/java/com/myorg/HomeController.java").exists());
            assertTrue(new File(root, ".atomist.yml").exists());
            FileUtils.deleteQuietly(root);
        }, "generate", "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", name,
                "root_package=my.test", (!location.equals(".") ? "-C" : null),
                (!location.equals(".") ? location : null));
    }

}
