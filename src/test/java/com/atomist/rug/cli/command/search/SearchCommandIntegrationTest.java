package com.atomist.rug.cli.command.search;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class SearchCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testFullArtifactFiltered() throws Exception {
        assertCommandLine(0,
                () -> assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                        .contains("atomist-rugs:spring-boot-rest-service")),
                "search", "docker", "-T", "spring", "-T", "lein");
    }
}
