package com.atomist.rug.cli.command.search;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class SearchCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testFullArtifactFilteredByTag() throws Exception {
        assertCommandLine(0,
                () -> assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                        .contains("atomist-rugs:spring-boot-editors")),
                "search", "docker", "-T", "spring");
    }
    
    @Test
    public void testFullArtifactFilteredByType() throws Exception {
        assertCommandLine(0,
                () -> assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                        .contains("atomist-rugs:spring-boot-editors")),
                "search", "docker", "--type", "editor");
    }

    @Test
    public void testFullArtifactFilteredByTypeAndTag() throws Exception {
        assertCommandLine(0,
                () -> assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                        .contains("atomist-rugs:spring-boot-editors")),
                "search", "docker", "--type", "editor", "--tag", "spring");
    }
}
