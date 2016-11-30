package com.atomist.rug.cli;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TestName;
import org.springframework.util.StringUtils;

public abstract class AbstractCommandTest {

    @Rule
    public ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public TestName name = new TestName();

    @Rule
    public SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Before
    public void setup() {
        System.setProperty("jansi.strip", "true");
        systemOutRule.clearLog();
        System.out.println("");
        System.out.println(">>> " + getClass().getSimpleName() + "." + name.getMethodName());
    }

    protected void assertCommandLine(int exitCode, Assertion assertion, String... tokens)
            throws Exception {
        assertCommandLine(exitCode, assertion, true, tokens);
    }

    protected void assertCommandLine(int exitCode, Assertion assertion, boolean includeConf,
            String... tokens) throws Exception {
        String[] commandLine = commandLine(includeConf, tokens);
        System.out.println(">>> " + Constants.COMMAND + " "
                + StringUtils.arrayToDelimitedString(commandLine, " "));
        System.out.println("");
        systemOutRule.clearLog();

        exit.expectSystemExitWithStatus(exitCode);
        exit.checkAssertionAfterwards(assertion);
        Main.main(commandLine);
    }

    protected void assertCommandLine(int exitCode, String requiredContents, String... tokens)
            throws Exception {
        assertCommandLine(exitCode, new SystemOutAssertion(requiredContents), tokens);
    }

    protected void assertFailure(String requiredContents, String... tokens) throws Exception {
        assertCommandLine(1, requiredContents, tokens);
    }

    protected void assertSuccess(String requiredContents, String... tokens) throws Exception {
        assertCommandLine(0, requiredContents, tokens);
    }

    protected String[] commandLine(boolean includeConf, String... tokens) {
        File config = new File("../cli.yml");

        List<String> commandLine = new ArrayList<>(Arrays.asList(tokens));
        if (includeConf) {
            commandLine.add("-q");
            commandLine.add("-s");
            commandLine.add(config.getAbsolutePath());
            commandLine.add("-X");
        }
        commandLine = commandLine.stream().filter(c -> c != null).collect(Collectors.toList());

        return commandLine.toArray(new String[commandLine.size()]);
    }

    protected class SystemOutAssertion implements Assertion {

        private String requiredContent;

        public SystemOutAssertion(String requiredContent) {
            this.requiredContent = requiredContent;
        }

        @Override
        public void checkAssertion() throws Exception {
            assertTrue(systemOutRule.getLogWithNormalizedLineSeparator().contains(requiredContent)
                    || systemErrRule.getLogWithNormalizedLineSeparator().contains(requiredContent));
        }

    }

}
