package com.atomist.rug.cli;

import static org.junit.Assert.assertTrue;

import com.atomist.project.archive.Rugs;
import com.atomist.rug.cli.command.CommandContext;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.compiler.typescript.TypeScriptCompiler;
import com.atomist.source.ArtifactSource;
import com.github.tomaslanger.chalk.Chalk;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TestName;
import org.springframework.util.StringUtils;

public abstract class AbstractCommandTest {

    private static String startingUserDir = getCWD();

    @Rule
    public ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public TestName name = new TestName();

    @Rule
    public SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @BeforeClass
    public static void setup() {
        System.setProperty("jansi.strip", "true");
    }

    @Before
    public void setupRules() {
        Chalk.setColorEnabled(false);
        systemOutRule.clearLog();
        System.out.println("");
        System.out.println(">>> " + getClass().getSimpleName() + "." + name.getMethodName());
        CommandContext.delete(ArtifactSource.class);
        CommandContext.delete(Rugs.class);
        CommandContext.delete(TypeScriptCompiler.class);
        setRelativeCWD("src/test/resources/common-editors");
    }

    protected void assertCommandLine(int exitCode, Assertion assertion, String... tokens)
            throws Exception {
        assertCommandLine(exitCode, assertion, true, tokens);
    }

    protected void assertCommandLine(int exitCode, Assertion assertion, boolean includeConf,
            String... tokens) throws Exception {
        String[] commandLine = commandLine(includeConf, tokens);
        printCWD();
        System.out.println(">>> " + Constants.command()
                + StringUtils.arrayToDelimitedString(commandLine, " "));
        System.out.println("");
        systemOutRule.clearLog();

        exit.expectSystemExitWithStatus(exitCode);
        exit.checkAssertionAfterwards(assertion);
        try {
            Main.main(commandLine);
        }
        catch (Throwable t) {
            Chalk.setColorEnabled(true);
            throw t;
        }
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
        File config = new File(getCWD(), "../cli.yml");
        if (!config.exists()) {
            throw new RuntimeException(
                    "Config file missing at ../cli.yml\nLook, if you're running tests locally, try setting your working directory to src/test/resources/common-editors");
        }

        List<String> commandLine = new ArrayList<>(Arrays.asList(tokens));
        if (includeConf) {
            commandLine.add("-q");
            commandLine.add("-s");
            commandLine.add(config.getAbsolutePath());
            commandLine.add("-X");
            commandLine.add("-t");
            // commandLine.add("-V");
        }
        commandLine = commandLine.stream().filter(Objects::nonNull).collect(Collectors.toList());

        return commandLine.toArray(new String[commandLine.size()]);
    }

    public static void printCWD() {
        System.out.println(">>> user dir: " + System.getProperty("user.dir"));
    }

    public static String getCWD() {
        return System.getProperty("user.dir");
    }

    public static void setCWD(String dir) {
        System.setProperty("user.dir", dir);
    }

    public void setRelativeCWD(String dir) {
        setCWD(startingUserDir + "/" + dir);
    }

    protected class SystemOutAssertion implements Assertion {

        private String requiredContent;

        public SystemOutAssertion(String requiredContent) {
            this.requiredContent = requiredContent;
        }

        @Override
        public void checkAssertion() throws Exception {

            String sysout = systemOutRule.getLogWithNormalizedLineSeparator();
            String stderr = systemErrRule.getLogWithNormalizedLineSeparator();
            if (!sysout.contains(requiredContent) && !stderr.contains(requiredContent)) {
                // re-enable color to see an error message flying by
                System.setProperty("jansi.strip", "false");
                Chalk.setColorEnabled(true);
                System.out.println(Style
                        .red("Received on sysout: <\n" + sysout + "\n> and on stderr: <\n" + stderr
                                + "\n> neither of which contain: <\n" + requiredContent + "\n>"));
                System.setProperty("jansi.strip", "true");
                Chalk.setColorEnabled(false);
            }
            assertTrue(sysout.contains(requiredContent) || stderr.contains(requiredContent));
        }
    }
}
