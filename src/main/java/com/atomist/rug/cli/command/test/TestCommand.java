package com.atomist.rug.cli.command.test;

import java.util.Collections;
import java.util.List;

import javax.script.SimpleBindings;

import com.atomist.project.archive.DefaultAtomistConfig$;
import com.atomist.project.archive.Rugs;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.runtime.js.JavaScriptContext;
import com.atomist.rug.test.gherkin.ArchiveTestResult;
import com.atomist.rug.test.gherkin.GherkinRunner;
import com.atomist.rug.test.gherkin.TestReport;
import com.atomist.source.ArtifactSource;

import scala.collection.JavaConverters;

public class TestCommand extends AbstractAnnotationBasedCommand {

    private Log log = new Log(getClass());

    @Command
    public void run(Rugs operations, ArtifactDescriptor artifact, ArtifactSource source) {
        List<String> bla = Collections.emptyList();

        ArchiveTestResult result = new ProgressReportingOperationRunner<ArchiveTestResult>(
                String.format("Running tests in %s", ArtifactDescriptorUtils.coordinates(artifact)))
                        .run((indicator) -> {
                            GherkinRunner runner = new GherkinRunner(new JavaScriptContext(source,
                                    DefaultAtomistConfig$.MODULE$, new SimpleBindings(),
                                    JavaConverters.asScalaBufferConverter(bla).asScala()));
                            return runner.execute();
                        });
        TestReport report = new TestReport(result);
        log.newline();

        if (result.passed()) {
            log.info(Style.green(String.format("Successfully executed %s of %s %s: Test SUCCESS",
                    result.featureResults().size(), result.testCount(),
                    (result.testCount() > 1 ? "tests" : "test"))));
        }
        else {
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Test Report"));

            if (report.failures().nonEmpty()) {
                log.info(Style.yellow("  Failures"));
                JavaConverters.asJavaCollectionConverter(report.failures()).asJavaCollection()
                        .forEach(f -> {
                            log.info("    " + f.feature().getName() + ": "
                                    + Style.red(f.result().message()));
                        });
            }
            if (report.notYetImplemented().nonEmpty()) {
                log.info(Style.yellow("  Not yet implemented"));
                JavaConverters.asJavaCollectionConverter(report.notYetImplemented())
                        .asJavaCollection().forEach(f -> {
                            log.info("   " + f.feature().getName());
                        });
            }

            throw new CommandException(String.format(
                    "Unsuccessfully executed %s of %s %s: Test FAILURE", report.failures().size(),
                    result.testCount(), (result.testCount() > 1 ? "tests" : "test")));
        }
    }
}
