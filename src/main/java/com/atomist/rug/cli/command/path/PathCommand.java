package com.atomist.rug.cli.command.path;

import com.atomist.graph.GraphNode;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.CommandException;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.annotation.Validator;
import com.atomist.rug.cli.command.utils.ArtifactSourceUtils;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.tree.TreeNodeTreeCreator;
import com.atomist.rug.cli.utils.FileUtils;
import com.atomist.rug.cli.utils.StringUtils;
import com.atomist.rug.kind.DefaultTypeRegistry$;
import com.atomist.rug.kind.core.ProjectMutableView;
import com.atomist.source.ArtifactSource;
import com.atomist.source.EmptyArtifactSource;
import com.atomist.tree.TreeNode;
import com.atomist.tree.pathexpression.ExpressionEngine;
import com.atomist.tree.pathexpression.PathExpression;
import com.atomist.tree.pathexpression.PathExpressionEngine;
import com.atomist.tree.pathexpression.PathExpressionParser$;
import com.atomist.tree.utils.NodeUtils;
import scala.Option$;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.util.Either;

import java.io.File;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class PathCommand extends AbstractAnnotationBasedCommand {
    
    @Validator
    public void validate(@Option("change-dir") String projectName) {
        File root = FileUtils.createProjectRoot(projectName);
        if (!root.exists()) {
            throw new CommandException(String.format(
                    "Target directory %s does not exist.\nPlease fix the directory path provided to --change-dir.",
                    projectName), "path");
        }
        if (!root.isDirectory()) {
            throw new CommandException(String.format(
                    "Target path %s is not a directory.\nPlease fix the directory path provided to --change-dir.",
                    projectName), "path");
        }
    }

    @Command
    public void run(@Argument(index = 1, defaultValue = "") String expression,
            @Option("change-dir") String rootName, @Option("values") boolean values) {

        Collection<GraphNode> treeNodes = new ProgressReportingOperationRunner<Collection<GraphNode>>(
                "Evaluating path expression against project").run((indicator) -> {

                    PathExpression pathExpression = PathExpressionParser$.MODULE$
                            .parseString(expression);

                    File root = FileUtils.createProjectRoot(rootName);
                    ArtifactSource source = ArtifactSourceUtils.createArtifactSource(root);

                    ExpressionEngine pxe = new PathExpressionEngine();
                    TreeNode pmv = new ProjectMutableView(new EmptyArtifactSource(""), source);

                    Either<String, Seq<GraphNode>> result = pxe.evaluate(pmv, pathExpression,
                            DefaultTypeRegistry$.MODULE$, Option$.MODULE$.apply(null));

                    return JavaConverters.asJavaCollectionConverter(result.right().get())
                            .asJavaCollection();
                });

        printResult(expression, values, treeNodes);
    }

    protected void printResult(String expression, boolean values, Collection<GraphNode> treeNodes) {
        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Path Expression"));
        log.info("  %s", expression);
        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " "
                + Style.bold(StringUtils.puralize("Match", "Matches", treeNodes)) + " ("
                + treeNodes.size() + " found)");
        if (!treeNodes.isEmpty()) {
            TreeNodeTreeCreator.visitTree(treeNodes,
                    (values ? new ValueNodeToStringFunction() : new NodeToStringFunction()),
                    new LogVisitor(log));
        }
        else {
            log.info(Style.yellow("  No matches"));
        }
        log.newline();
        log.info(Style.green("Successfully evaluated path expression"));
    }

    private static class ValueNodeToStringFunction
            implements BiFunction<Integer, GraphNode, String> {

        @Override
        public String apply(Integer id, GraphNode node) {
            return Style
                    .yellow(node
                            .nodeName())
                    + Style.gray(" [" + org.springframework.util.StringUtils
                            .collectionToDelimitedString(JavaConverters
                                    .asJavaCollectionConverter(node.nodeTags()).asJavaCollection()
                                    .stream().filter(n -> !n.equals("-dynamic"))
                                    .collect(Collectors.toList()), ", ")
                            + "]")
                    + ((node.relatedNodes().size() > 0) && id > 0 ? " {" + id + "}" : "")
                    + " " + NodeUtils.value(node);
        }
    }

    private static class NodeToStringFunction implements BiFunction<Integer, GraphNode, String> {

        @Override
        public String apply(Integer id, GraphNode node) {
            return Style
                    .yellow(node
                            .nodeName())
                    + Style.gray(" [" + org.springframework.util.StringUtils
                            .collectionToDelimitedString(JavaConverters
                                    .asJavaCollectionConverter(node.nodeTags()).asJavaCollection()
                                    .stream().filter(n -> !n.equals("-dynamic"))
                                    .collect(Collectors.toList()), ", ")
                            + "]")
                    + ((node.relatedNodes().size() > 0) && id > 0 ? " {" + id + "}" : "");
        }
    }
}
