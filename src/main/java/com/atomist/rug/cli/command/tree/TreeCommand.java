package com.atomist.rug.cli.command.tree;

import java.io.File;
import java.util.Collection;
import java.util.function.Function;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
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

import scala.Option$;
import scala.collection.JavaConverters;
import scala.collection.immutable.List;
import scala.util.Either;

public class TreeCommand extends AbstractAnnotationBasedCommand {

    @Command
    public void run(@Argument(index = 1, defaultValue = "") String expression,
            @Option("change-dir") String rootName, @Option("values") boolean values) {
        
        Collection<TreeNode> treeNodes = new ProgressReportingOperationRunner<Collection<TreeNode>>(
                "Evaluating path expression against project").run((indicator) -> {

                    PathExpression pathExpression = PathExpressionParser$.MODULE$
                            .parseString(expression);

                    File root = FileUtils.createProjectRoot(rootName);
                    ArtifactSource source = ArtifactSourceUtils.createArtifactSource(root);

                    ExpressionEngine pxe = new PathExpressionEngine();
                    TreeNode pmv = new ProjectMutableView(new EmptyArtifactSource(""), source);

                    Either<String, List<TreeNode>> result = pxe.evaluate(pmv, pathExpression,
                            DefaultTypeRegistry$.MODULE$, Option$.MODULE$.apply(null));

                    return JavaConverters.asJavaCollectionConverter(result.right().get())
                            .asJavaCollection();
                });

        printResult(expression, values, treeNodes);
    }

    protected void printResult(String expression, boolean values, Collection<TreeNode> treeNodes) {
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
        log.info(Style.green("Successfully evaluated tree expression"));
    }

    private static class ValueNodeToStringFunction implements Function<TreeNode, String> {

        @Override
        public String apply(TreeNode node) {
            return node.nodeName() + ": ["
                    + org.springframework.util.StringUtils.collectionToDelimitedString(
                            JavaConverters.asJavaCollectionConverter(node.nodeTags())
                                    .asJavaCollection(),
                            ", ")
                    + "] " + node.value();
        }
    }

    private static class NodeToStringFunction implements Function<TreeNode, String> {

        @Override
        public String apply(TreeNode node) {
            return node.nodeName() + ": ["
                    + org.springframework.util.StringUtils.collectionToDelimitedString(
                            JavaConverters.asJavaCollectionConverter(node.nodeTags())
                                    .asJavaCollection(),
                            ", ")
                    + "]";
        }
    }
}
