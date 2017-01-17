package com.atomist.rug.cli.command.test;

import static scala.collection.JavaConversions.asJavaCollection;
import static scala.collection.JavaConversions.asScalaBuffer;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.atomist.project.archive.DefaultAtomistConfig$;
import com.atomist.project.archive.Operations;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.CommandUtils;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.tree.ArtifactSourceTreeCreator;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.test.RugTestParser;
import com.atomist.rug.test.TestLoader;
import com.atomist.rug.test.TestReport;
import com.atomist.rug.test.TestRunner;
import com.atomist.rug.test.TestScenario;
import com.atomist.source.ArtifactSource;
import com.atomist.source.FileArtifact;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.SimpleFileSystemArtifactSourceIdentifier;

import scala.collection.Seq;

public class TestCommand extends AbstractAnnotationBasedCommand {

    private Log log = new Log(getClass());

    @Command
    public void run(Operations operations, ArtifactDescriptor artifact,
            @Argument(index = 1) String testName) {

        File workingDir = CommandUtils.getRequiredWorkingDirectory();
//        ArtifactSource source = new FileSystemArtifactSource(
//                new SimpleFileSystemArtifactSourceIdentifier(workingDir),
//                Arrays.asList(new GitDirFilter(workingDir.getPath())));

        ArtifactSource source = new FileSystemArtifactSource(
                new SimpleFileSystemArtifactSourceIdentifier(workingDir));

        TestLoader testLoader = new TestLoader(DefaultAtomistConfig$.MODULE$);
        Seq<TestScenario> scenarios = testLoader.loadTestScenarios(source);
        TestReport report;

        // run all tests
        if (testName == null) {
            report = runTests(scenarios, source, artifact, operations);
        }
        else {
            // search for one scenario
            Optional<TestScenario> scenario = asJavaCollection(scenarios).stream()
                    .filter(s -> s.name().equals(testName)).findFirst();
            if (scenario.isPresent()) {
                report = runTests(asScalaBuffer(Collections.singletonList(scenario.get())), source,
                        artifact, operations);
            }
            else {
                // search for scenarios from a given file
                List<FileArtifact> testFiles = asJavaCollection(source.allFiles()).stream()
                        .filter(f -> DefaultAtomistConfig$.MODULE$.isRugTest(f) && f.name()
                                .equals(testName + DefaultAtomistConfig$.MODULE$.testExtension()))
                        .collect(Collectors.toList());

                if (!testFiles.isEmpty()) {
                    List<TestScenario> fileScenarios = testFiles.stream()
                            .flatMap(f -> asJavaCollection(RugTestParser.parse(f))

                                    .stream())
                            .collect(Collectors.toList());
                    report = runTests(asScalaBuffer(fileScenarios), source, artifact, operations);

                }
                else {
                    throw new CommandException(String.format(
                            "Specified test scenario or test file %s could not be found.",
                            testName));
                }
            }
        }

        log.newline();
        if ((report != null) && (report.passed())) {
            log.info(Style
                    .green(String.format("Successfully executed %s of %s scenarios: Test SUCCESS",
                            report.passedTests().size(), report.tests().size())));
        }
        else {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Failed Scenarios"));
            asJavaCollection(report.failures()).forEach(t -> {
                log.info(Style.yellow("  %s", t.name())
                        + String.format(" (%s of %s assertions failed)", t.failures().size(),
                                t.assertions().size()));
                log.info("    " + Style.underline("Failed Assertions"));
                asJavaCollection(t.failures()).forEach(a -> log.info("       %s", a.message()));
                if (t.eventLog().input().isDefined()) {
                    log.info("    " + Style.underline("Input"));
                    ArtifactSourceTreeCreator.visitTree(t.eventLog().input().getOrElse(null),
                            new LogVisitor(log, "    "));
                }
                if (t.eventLog().output().isDefined()) {
                    log.info("    " + Style.underline("Ouput"));
                    ArtifactSourceTreeCreator.visitTree(t.eventLog().output().getOrElse(null),
                            new LogVisitor(log, "    "));
                }
            });
            throw new CommandException(
                    String.format("Unsuccessfully executed %s of %s scenarios: Test FAILED",
                            "" + report.failures().size(), "" + report.tests().size()));
        }
    }

    private TestReport runTests(Seq<TestScenario> scenarios, ArtifactSource source,
            ArtifactDescriptor artifact, Operations operations) {
        return new ProgressReportingOperationRunner<TestReport>(String.format(
                "Running test scenarios in %s", ArtifactDescriptorUtils.coordinates(artifact)))
                        .run(indicator -> {
                            TestRunner testRunner = new TestRunner(indicator::report);

                            return testRunner.run(scenarios, source, operations.allOperations(),
                                    scala.Option
                                            .apply(artifact.group() + "." + artifact.artifact()));

                        });

    }
}
