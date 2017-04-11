package com.atomist.rug.cli.command.dependencies;

import org.junit.Before;
import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;
import com.atomist.rug.cli.Constants;

public class DependenciesCommandIntegrationTest extends AbstractCommandTest {
    
    @Before
    public void before() throws Exception {
        setRelativeCWD("src/test/resources/handlers");
    }

    @Test
    public void testHelp() throws Exception {
        assertSuccess("Usage: rug dependencies [OPTION]... ARTIFACT", "dependencies", "-h");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersion() throws Exception {
        assertSuccess("atomist-rugs:spring-boot-rest-service (0.10.0" + Constants.DOT + "zip)",
                "deps",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", "-a", "0.10.0");
    }

    @Test
    public void testSuccessfulDependencies() throws Exception {
        assertSuccess("rug-cli-tests:handlers (0.12.0)", "deps",
                "-l", "--operations");
    }

}
