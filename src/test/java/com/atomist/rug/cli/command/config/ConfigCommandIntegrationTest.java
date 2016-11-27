package com.atomist.rug.cli.command.config;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.atomist.rug.cli.AbstractCommandTest;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConfigCommandIntegrationTest extends AbstractCommandTest {

    @Test
    public void testAGlobalWrite() throws Exception {
        assertCommandLine(0, () -> {
        }, "default", "--global", "atomist:cd", "-a", "100.0.0");
    }

    @Test
    public void testBProjectWrite() throws Exception {

        assertCommandLine(0, () -> {
        }, "default", "atomist:cd", "-a", "100.0.0");
    }

    @Test
    public void testCGlobalDelete() throws Exception {

        assertCommandLine(0, () -> {
        }, "default", "--global", "--delete");
    }

    @Test
    public void testDProjectDelete() throws Exception {

        assertCommandLine(0, () -> {
        }, "default", "--delete");
    }
}
