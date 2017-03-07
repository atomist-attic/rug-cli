package com.atomist.rug.cli.command.test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.script.SimpleBindings;

import org.springframework.util.ClassUtils;

import com.atomist.project.archive.DefaultAtomistConfig$;
import com.atomist.project.archive.Rugs;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.tree.Node;
import com.atomist.rug.cli.tree.Node.Type;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.runtime.js.JavaScriptContext;
import com.atomist.rug.test.gherkin.ArchiveTestResult;
import com.atomist.rug.test.gherkin.AssertionResult;
import com.atomist.rug.test.gherkin.Failed;
import com.atomist.rug.test.gherkin.FeatureResult;
import com.atomist.rug.test.gherkin.GherkinRunner;
import com.atomist.rug.test.gherkin.ScenarioResult;
import com.atomist.rug.test.gherkin.TestReport;
import com.atomist.source.ArtifactSource;

import scala.Option;
import scala.collection.JavaConverters;

public class TestCommand extends AbstractAnnotationBasedCommand {

    private Log log = new Log(getClass());

    @Command
    public void run(Rugs operations, ArtifactDescriptor artifact, ArtifactSource source) {
        List<String> bla = Collections.emptyList();

        ArchiveTestResult result = new ProgressReportingOperationRunner<ArchiveTestResult>(
                String.format("Running tests in %s", ArtifactDescriptorUtils.coordinates(artifact)))
                        .run((indicator) -> {
                            GherkinRunner runner = new GherkinRunner(
                                    new JavaScriptContext(source, DefaultAtomistConfig$.MODULE$,
                                            new SimpleBindings(),
                                            JavaConverters.asScalaBufferConverter(bla).asScala()),
                                    Option.apply(operations));
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
                toTree(JavaConverters.asJavaCollectionConverter(report.failures())
                        .asJavaCollection());
            }
            if (report.notYetImplemented().nonEmpty()) {
                log.info(Style.yellow("  Not yet implemented"));
                toTree(JavaConverters.asJavaCollectionConverter(report.notYetImplemented())
                        .asJavaCollection());
            }

            throw new CommandException(String.format(
                    "Unsuccessfully executed %s of %s %s: Test FAILURE", report.failures().size(),
                    result.testCount(), (result.testCount() > 1 ? "tests" : "test")));
        }
    }

    private String resultName(AssertionResult result) {
        String name = ClassUtils.getShortName(result.result().getClass());
        if (name.endsWith(".")) {
            name = name.substring(0, name.length() - 1);
        }
        switch (name) {
        case "Passed":
            name = Style.green(name);
            break;
        case "NotYetImplemented":
            name = Style.yellow(name);
            break;
        case "Failed":
            name = Style.red(name);
            break;
        }
        return name;
    }

    private void toTree(Collection<FeatureResult> results) {
        Node root = new Node(null);

        results.forEach(r -> addScenrioResults(
                root.addChild(r.feature().getName(), Node.Type.UNKNOWN),
                JavaConverters.asJavaCollectionConverter(r.scenarioResults()).asJavaCollection()));

        LogVisitor visitor = new LogVisitor(log);
        root.accept(visitor);
    }

    private void addScenrioResults(Node node, Collection<ScenarioResult> scenarioResults) {
        scenarioResults.forEach(
                sr -> addAssertion(node.addChild(sr.scenario().getName(), Node.Type.UNKNOWN),
                        JavaConverters.asJavaCollectionConverter(sr.results()).asJavaCollection()));
    }

    private void addAssertion(Node node, Collection<AssertionResult> assertionResults) {
        assertionResults.forEach(ar -> {
            Node assertion = node.addChild(ar.assertion() + ": " + resultName(ar), Node.Type.UNKNOWN);
            if (ar.result() instanceof Failed) {
                assertion.addChild(Style.gray(ar.result().message()), Type.UNKNOWN);
            }
        });
    }
}
