package com.atomist.rug.cli.command.list;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class ListCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testFullArtifactFiltered() throws Exception {
        assertCommandLine(0,
                () -> assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                        .contains("atomist-rugs:spring-boot-rest-service")),
                "list", "-f", "artifact=spring-boot-r?st*");
    }

    @Test
    public void testFullListing() throws Exception {
        assertCommandLine(0, () -> {
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-rugs:common-editors"));
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-rugs:spring-boot-rest-service"));
        }, "list");
    }

    @Test
    public void testFullVersionAndArtifactFiltered() throws Exception {
        assertCommandLine(0,
                () -> assertFalse(systemOutRule.getLogWithNormalizedLineSeparator()
                        .contains("atomist-rugs:spring-boot-rest-service")),
                "list", "-f", "artifact=spring-boot-r?st*", "-f", "version=[0.2.0,2.6)");
    }

    @Test
    public void testGroupFiltered() throws Exception {
        assertCommandLine(0, () -> {
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-rugs:common-editors"));
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-rugs:spring-boot-rest-service"));
        }, "list", "-f", "group=*atomist?rugs");
    }

    @Test
    public void testVersionFiltered() throws Exception {
        assertCommandLine(0, () -> {
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-rugs:common-editors"));
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-rugs:spring-boot-rest-service"));
        }, "list", "-f", "version=[0.1.0,3.3)");
    }
}
