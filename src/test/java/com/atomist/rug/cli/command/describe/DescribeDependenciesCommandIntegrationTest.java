package com.atomist.rug.cli.command.describe;

import org.junit.Before;
import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;
import com.atomist.rug.cli.Constants;

public class DescribeDependenciesCommandIntegrationTest extends AbstractCommandTest {
    
    @Before
    public void before() throws Exception {
        setRelativeCWD("src/test/resources/handlers");
    }

    @Test
    public void testHelp() throws Exception {
        assertSuccess("Usage: rug describe [OPTION]... TYPE ARTIFACT", "describe", "dependencies", "-h");
    }

    @Test
    public void testSuccessfulGeneratorDescribeWithVersion() throws Exception {
        assertSuccess("atomist-rugs:spring-boot-rest-service (0.10.0" + Constants.DOT + "zip)",
                "describe", "dependencies",
                "atomist-rugs:spring-boot-rest-service:NewSpringBootRestService", "-a", "0.10.0");
    }

    @Test
    public void testSuccessfulDependencies() throws Exception {
        assertSuccess("rug-cli-tests:handlers (0.12.0)", "describe", "dependencies",
                "-l", "--operations");
    }

}
