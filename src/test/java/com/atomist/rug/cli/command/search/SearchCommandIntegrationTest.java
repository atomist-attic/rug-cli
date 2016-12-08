package com.atomist.rug.cli.command.search;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class SearchCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testFullArtifactFiltered() throws Exception {
        assertCommandLine(0,
                () -> assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                        .contains("atomist-project-templates:spring-boot-common-editors (1.8.2)")),
                "search", "docker", "-T", "spring");
    }
}
