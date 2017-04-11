package com.atomist.rug.cli.command.test;

import java.io.PrintWriter;
import java.io.StringWriter;
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
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.output.ConsoleLogger;
import com.atomist.rug.cli.output.ProgressReporter;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.tree.Node;
import com.atomist.rug.cli.tree.Node.Type;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.cli.utils.CommandLineOptions;
import com.atomist.rug.cli.utils.Timing;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.runtime.js.JavaScriptContext;
import com.atomist.rug.test.gherkin.ArchiveTestResult;
import com.atomist.rug.test.gherkin.AssertionResult;
import com.atomist.rug.test.gherkin.Failed;
import com.atomist.rug.test.gherkin.FeatureDefinition;
import com.atomist.rug.test.gherkin.FeatureResult;
import com.atomist.rug.test.gherkin.GherkinExecutionListener;
import com.atomist.rug.test.gherkin.GherkinRunner;
import com.atomist.rug.test.gherkin.PathExpressionEvaluation;
import com.atomist.rug.test.gherkin.ScenarioResult;
import com.atomist.rug.test.gherkin.TestReport;
import com.atomist.source.ArtifactSource;

import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Step;
import scala.Option;
import scala.collection.JavaConverters;
import scala.runtime.AbstractFunction1;

public class TestCommand extends AbstractAnnotationBasedCommand {

    private Log log = new Log(getClass());

    @Command
    public void run(Rugs operations, ArtifactDescriptor artifact, ArtifactSource source,
            @Argument(index = 1) String test) {

        ArchiveTestResult result = new ProgressReportingOperationRunner<ArchiveTestResult>(
                String.format("Running tests in %s", ArtifactDescriptorUtils.coordinates(artifact)))
                        .run((indicator) -> {
                            List<GherkinExecutionListener> listeners = Collections
                                    .singletonList(new LoggingGherkinExecutionListener(indicator));
                            GherkinRunner runner = new GherkinRunner(
                                    new JavaScriptContext(source, DefaultAtomistConfig$.MODULE$,
                                            new SimpleBindings(),
                                            ConsoleLogger.consoleLogger()),
                                    Option.apply(operations),
                                    JavaConverters.asScalaBufferConverter(listeners).asScala());

                            return runner
                                    .execute(new AbstractFunction1<FeatureDefinition, Object>() {
                                        @Override
                                        public Object apply(FeatureDefinition fd) {
                                            if (test == null) {
                                                return true;
                                            }
                                            else {
                                                return test.equals(fd.feature().getName())
                                                        || test.equals(fd.definition().name());
                                            }
                                        }
                                    });
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

        LogVisitor visitor = new LogVisitor();
        root.accept(visitor);
        visitor.log(log);
    }

    private void addScenrioResults(Node node, Collection<ScenarioResult> scenarioResults) {
        scenarioResults.forEach(
                sr -> addAssertion(node.addChild(sr.scenario().getName(), Node.Type.UNKNOWN),
                        JavaConverters.asJavaCollectionConverter(sr.results()).asJavaCollection()));
    }

    private void addAssertion(Node node, Collection<AssertionResult> assertionResults) {
        assertionResults.forEach(ar -> {
            Node assertion = node.addChild(ar.assertion() + ": " + resultName(ar),
                    Node.Type.UNKNOWN);
            if (ar.result() instanceof Failed) {
                String message = ar.result().message();
                assertion.addChild(message, Type.DETAIL);
            }
        });
    }

    private static class LoggingGherkinExecutionListener implements GherkinExecutionListener {

        private final ProgressReporter reporter;

        private Timing featureTimer;
        private Timing scenarioTimer;

        public LoggingGherkinExecutionListener(ProgressReporter reporter) {
            this.reporter = reporter;
        }

        @Override
        public void featureStarting(FeatureDefinition fd) {
            featureTimer = new Timing();
            reporter.report("Running test feature " + fd.feature().getName());
        }

        @Override
        public void scenarioStarting(ScenarioDefinition sd) {
            scenarioTimer = new Timing();
            reporter.report("  Running test scenario " + sd.getName());
        }

        @Override
        public void scenarioCompleted(ScenarioDefinition sd, ScenarioResult sr) {
            String duration = String.format("%.2f", scenarioTimer.duration());
            String msg = "  Completed test scenario " + sd.getName() + " "
                    + (sr.passed() ? Style.green("passed") : Style.red("failed"));
            if (CommandLineOptions.hasOption("t")) {
                msg = msg + " in " + duration + "s";
            }
            reporter.report(msg);
        }

        @Override
        public void featureCompleted(FeatureDefinition fd, FeatureResult fr) {
            String duration = String.format("%.2f", featureTimer.duration());
            String msg = "Completed test feature " + fd.feature().getName() + " "
                    + (fr.passed() ? Style.green("passed") : Style.red("failed"));
            if (CommandLineOptions.hasOption("t")) {
                msg = msg + " in " + duration + "s";
            }
            reporter.report(msg);
        }

        @Override
        public void stepCompleted(Step s) {
        }

        @Override
        public void stepFailed(Step s, Throwable t) {
            if (CommandLineOptions.hasOption("X")) {
                StringWriter errors = new StringWriter();
                t.printStackTrace(new PrintWriter(errors));
                reporter.report("    Step " + s.getText() + " " + Style.red("failed") + ":\n"
                        + errors.toString());
            }
            else {
                reporter.report("    Step " + s.getText() + " " + Style.red("failed") + ":    \n"
                        + t.getMessage());
            }
        }

        @Override
        public void stepStarting(Step s) {
        }

        @Override
        public void pathExpressionResult(PathExpressionEvaluation pxe) {
        }
    }
}
