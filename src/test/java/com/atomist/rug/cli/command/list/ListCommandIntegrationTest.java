package com.atomist.rug.cli.command.list;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.atomist.rug.cli.AbstractCommandTest;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ListCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void test1Setup() throws Exception {
        assertSuccess("", "describe", "archive", "atomist-rugs:spring-boot-rest-service", "-a",
                "0.10.0");
    }

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
            // TODO add this back in after next CLI release
            // assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
            // .contains("atomist-rugs:common-editors"));
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-rugs:spring-boot-rest-service"));
        }, "list");
    }

    @Test
    public void testFullVersionAndArtifactFiltered() throws Exception {
        assertCommandLine(0,
                () -> assertFalse(systemOutRule.getLogWithNormalizedLineSeparator()
                        .contains("atomist-rugs:spring-boot-rest-service")),
                "list", "-f", "artifact=spring-boot-r?st*", "-f", "version=[0.4.0,0.5.0)");
    }

    @Test
    public void testGroupFiltered() throws Exception {
        assertCommandLine(0, () -> {
            // assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
            // .contains("atomist-rugs:common-editors"));
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-rugs:spring-boot-rest-service"));
        }, "list", "-f", "group=*atomist?rugs");
    }

    @Test
    public void testVersionFiltered() throws Exception {
        assertCommandLine(0, () -> {
            // assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
            // .contains("atomist-rugs:common-editors"));
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator()
                    .contains("atomist-rugs:spring-boot-rest-service"));
        }, "list", "-f", "version=[0.5.0,3.3)");
    }
}
