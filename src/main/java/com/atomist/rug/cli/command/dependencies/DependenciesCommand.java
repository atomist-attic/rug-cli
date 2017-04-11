package com.atomist.rug.cli.command.dependencies;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.atomist.project.archive.Coordinate;
import com.atomist.project.archive.ResolvedDependency;
import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.RunnerException;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.tree.Node;
import com.atomist.rug.cli.tree.Node.Type;
import com.atomist.rug.runtime.Rug;
import com.atomist.rug.runtime.plans.DefaultRugFunctionRegistry;
import com.atomist.rug.spi.RugFunction;

import scala.collection.JavaConverters;

public class DependenciesCommand extends AbstractAnnotationBasedCommand {

    @Command
    public void run(ResolvedDependency rugs, Settings settings,
            @Option(value = "operations") boolean operations) {

        File repo = new File(settings.getLocalRepository().path());
        URI repoHome = repo.toURI();

        Coordinate coordinate = rugs.address().get();

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

        
        Map<String, RugFunction> functions = JavaConverters
                .mapAsJavaMapConverter(DefaultRugFunctionRegistry.providerMap()).asJava();
        Map<URL, List<RugFunction>> extensions = functions.values().stream().collect(Collectors
                .groupingBy(f -> f.getClass().getProtectionDomain().getCodeSource().getLocation()));

        if (extensions.size() > 0) {
            log.newline();
            log.info(Style.cyan(Constants.DIVIDER) + " " + Style.bold("Extensions"));

            log.info("  " + formatCoordinate(coordinate));
            Node extenstionRoot = new Node(null);
            extensions.forEach((k, v) -> processFunctions(extractFromUrl(k, repoHome), v,
                    extenstionRoot, operations));
            visitor = new LogVisitor();
            extenstionRoot.accept(visitor);
            visitor.log(log);
        }

        log.newline();
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
        addOperation(node,
                JavaConverters.asJavaCollectionConverter(r.rugs().reviewers()).asJavaCollection(),
                "Reviewers");
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

    private Coordinate extractFromUrl(URL url, URI repoHome) {
        try {
            URI relativeUri = repoHome.relativize(url.toURI());
            List<String> segments = new ArrayList<>(
                    Arrays.asList(relativeUri.toString().split("/")));
            // last segment is the actual file name
            segments.remove(segments.size() - 1);
            // last segments is version
            String v = segments.remove(segments.size() - 1);
            // second to last is artifact
            String a = segments.remove(segments.size() - 1);
            // remaining segments are group
            String g = StringUtils.collectionToDelimitedString(segments, ".");
            return new Coordinate(g, a, v);
        }
        catch (URISyntaxException e) {
            throw new RunnerException(e);
        }
    }
}
