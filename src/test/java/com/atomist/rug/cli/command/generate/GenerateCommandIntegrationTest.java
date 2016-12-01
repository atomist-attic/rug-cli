package com.atomist.rug.cli.command.generate;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class GenerateCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testSuccessfulGenerate() throws Exception {
        String id = System.currentTimeMillis() + "";
        testGenerationAt(id, ".");
    }

    @Test
    public void testSuccessfulGenerateAtLocation() throws Exception {
        String id = System.currentTimeMillis() + "";
        testGenerationAt(id, System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void testUnSuccessfulGenerateWithInvalidParameter() throws Exception {
        assertFailure(
                "Invalid parameter value\n  project_name = 1234567891234567891212345678912345678912",
                "generate",
                "atomist-project-templates:spring-rest-service:Spring Boot Rest Microservice",
                "1234567891234567891212345678912345678912");
    }

    @Test
    public void testUnSuccessfulGenerateWithMissingParameter() throws Exception {
        assertFailure("Missing parameter value\n  project_name", "generate",
                "atomist-project-templates:spring-rest-service:Spring Boot Rest Microservice");
    }

    private void testGenerationAt(String name, String location) throws Exception {
        assertCommandLine(0, () -> {
            File root = new File(location, name);
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("Successfully generated new project " + name));
            assertTrue(new File(root, "src/main/java/my/test/HomeController.java").exists());
            assertTrue(new File(root, ".provenance.txt").exists());
            FileUtils.deleteQuietly(root);
        }, "generate",
                "atomist-project-templates:spring-rest-service:Spring Boot Rest Microservice", name,
                "root_package=my.test", (!location.equals(".") ? "-C" : null),
                (!location.equals(".") ? location : null));
    }

}
