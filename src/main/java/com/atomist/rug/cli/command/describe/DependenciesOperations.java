package com.atomist.rug.cli.command.describe;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.atomist.project.archive.Coordinate;
import com.atomist.project.archive.ResolvedDependency;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.Log;
import com.atomist.rug.cli.command.utils.OperationUtils;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.tree.Node;
import com.atomist.rug.cli.tree.Node.Type;
import com.atomist.rug.cli.utils.ArtifactDescriptorUtils;
import com.atomist.rug.resolver.ArtifactDescriptor;
import com.atomist.rug.runtime.Rug;
import com.atomist.rug.runtime.plans.DefaultRugFunctionRegistry;
import com.atomist.rug.spi.RugFunction;

import scala.collection.JavaConverters;

public class DependenciesOperations {
    
    private static final Log log = new Log(DependenciesOperations.class);

    public void run(ArtifactDescriptor artifact, ResolvedDependency rugs, Settings settings,
            boolean operations) {
        Coordinate coordinate = rugs.address().get();

        Map<String, RugFunction> functions = new ProgressReportingOperationRunner<Map<String, RugFunction>>(
                String.format("Loading extensions for %s",
                        ArtifactDescriptorUtils.coordinates(artifact))).run(indicator -> {
                            return JavaConverters
                                    .mapAsJavaMapConverter(DefaultRugFunctionRegistry.providerMap())
                                    .asJava();
                        });

        log.newline();
        log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Dependencies"));

        log.info("  " + formatCoordinate(coordinate));

        Node root = new Node(null);
        if (operations) {
            addOperations(root, rugs);
        }
        processDependencies(
                JavaConverters.asJavaCollectionConverter(rugs.dependencies()).asJavaCollection(),
                root, operations);
        LogVisitor visitor = new LogVisitor();
        root.accept(visitor);
        visitor.log(log);

        Map<URL, List<RugFunction>> extensions = functions.values().stream().collect(Collectors
                .groupingBy(f -> f.getClass().getProtectionDomain().getCodeSource().getLocation()));

        if (extensions.size() > 0) {
            log.newline();
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Extensions"));

            log.info("  " + formatCoordinate(coordinate));
            Node extenstionRoot = new Node(null);
            extensions.forEach((k, v) -> processFunctions(OperationUtils.extractFromUrl(k), v,
                    extenstionRoot, operations));
            visitor = new LogVisitor();
            extenstionRoot.accept(visitor);
            visitor.log(log);
        }
    }

    private void processDependencies(Collection<ResolvedDependency> rugs, Node node,
            boolean operations) {
        rugs.forEach(r -> {
            Coordinate coordinate = r.address().get();
            Node rugNode = node.addChild(formatCoordinate(coordinate), Type.UNKNOWN);
            if (operations) {
                addOperations(rugNode, r);
            }
            processDependencies(
                    JavaConverters.asJavaCollectionConverter(r.dependencies()).asJavaCollection(),
                    rugNode, operations);
        });
    }

    private void processFunctions(Coordinate coordinate, Collection<RugFunction> functions,
            Node node, boolean operations) {
        Node rugNode = node.addChild(formatCoordinate(coordinate), Type.UNKNOWN);
        if (operations) {
            Node headerNode = rugNode.addChild(Style.bold("Functions"), Type.UNKNOWN);
            functions.forEach(f -> {
                headerNode.addChild(f.name(), Type.UNKNOWN);
            });
        }
    }

    protected void addOperations(Node node, ResolvedDependency r) {
        addOperation(node,
                JavaConverters.asJavaCollectionConverter(r.rugs().generators()).asJavaCollection(),
                "Generators");
        addOperation(node,
                JavaConverters.asJavaCollectionConverter(r.rugs().editors()).asJavaCollection(),
                "Editors");
        addOperation(node, JavaConverters.asJavaCollectionConverter(r.rugs().commandHandlers())
                .asJavaCollection(), "Command Handlers");
        addOperation(node, JavaConverters.asJavaCollectionConverter(r.rugs().eventHandlers())
                .asJavaCollection(), "Event Handlers");
        addOperation(node, JavaConverters.asJavaCollectionConverter(r.rugs().responseHandlers())
                .asJavaCollection(), "Response Handlers");
    }

    private void addOperation(Node node, Collection<?> operations, String label) {
        if (!operations.isEmpty()) {
            Node parent = node.addChild(Style.bold(label), Type.UNKNOWN);
            operations.forEach(o -> parent.addChild(((Rug) o).name(), Type.UNKNOWN));
        }
    }

    private String formatCoordinate(Coordinate coordinate) {
        return Style.yellow("%s:%s", coordinate.group(), coordinate.artifact()) + " "
                + Style.gray("(%s)", coordinate.version());
    }
}
