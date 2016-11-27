package com.atomist.rug.cli.command.list;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.atomist.rug.cli.AbstractCommandTest;

public class ListCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testFullArtifactFiltered() throws Exception {
        assertCommandLine(0, () -> assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                .contains("atomist-project-templates:spring-rest-service")), "list", "-f", "artifact=spring-r?st*");
    }

    @Test
    public void testFullListing() throws Exception {
        assertCommandLine(0, () -> {
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-project-templates:common-editors"));
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-project-templates:spring-rest-service"));
        }, "list");
    }

    @Test
    public void testFullVersionAndArtifactFiltered() throws Exception {
        assertCommandLine(0, () -> assertFalse(systemOutRule.getLogWithNormalizedLineSeparator()
                .contains("atomist-project-templates:spring-rest-service")), "list", "-f", "artifact=spring-r?st*", "-f", "version=[1.0,2.6)");
    }

    @Test
    public void testGroupFiltered() throws Exception {
        assertCommandLine(0, () -> {
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-project-templates:common-editors"));
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-project-templates:spring-rest-service"));
        }, "list", "-f", "group=*atomist-project-temp?ates");
    }

    @Test
    public void testVersionFiltered() throws Exception {
        assertCommandLine(0, () -> {
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-project-templates:common-editors"));
            assertFalse(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-project-templates:spring-rest-service"));
        }, "list", "-f", "version=[1.0,2.6)");
    }
}
