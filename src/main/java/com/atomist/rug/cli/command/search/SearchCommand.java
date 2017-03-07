package com.atomist.rug.cli.command.search;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.atomist.rug.cli.Constants;
import com.atomist.rug.cli.command.AbstractAnnotationBasedCommand;
import com.atomist.rug.cli.command.annotation.Argument;
import com.atomist.rug.cli.command.annotation.Command;
import com.atomist.rug.cli.command.annotation.Option;
import com.atomist.rug.cli.command.search.SearchOperations.Archive;
import com.atomist.rug.cli.command.search.SearchOperations.Operation;
import com.atomist.rug.cli.output.ProgressReportingOperationRunner;
import com.atomist.rug.cli.output.Style;
import com.atomist.rug.cli.settings.Settings;
import com.atomist.rug.cli.tree.LogVisitor;
import com.atomist.rug.cli.tree.Node;
import com.atomist.rug.cli.tree.Node.Type;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unused")
public class SearchCommand extends AbstractAnnotationBasedCommand {

    public static final List<String> CATALOG_URL = Arrays
            .asList("https://api.atomist.com/catalog/operation/search");
    public static final String CATALOG_URL_KEY = "catalog-service-urls";

    private ObjectMapper mapper = new ObjectMapper();

    @Command
    public void run(Settings settings, @Argument(index = 1) String search,
            @Option("tag") Properties tags, @Option("type") String type,
            @Option("operations") boolean showOps) {

        Map<String, List<Operation>> operations = new ProgressReportingOperationRunner<Map<String, List<Operation>>>(
                "Searching catalogs").run(indicator -> {
                    List<Operation> results = getCatalogServiceUrls(settings).stream().map(u -> {
                        indicator.report("  Searching " + u);
                        return new SearchOperations().collectResults(u, search, type, tags,
                                settings);
                    }).flatMap(List::stream).collect(Collectors.toList());

                    return results.stream().collect(Collectors.groupingBy(o -> o.archive().key()));
                });

        log.newline();
        log.info(
                Style.cyan(Constants.DIVIDER) + " "
                        + Style.bold(
                                "Remote Archives")
                        + (operations.keySet().size() > 0 ? " (" + operations.keySet().size() + " "
                                + com.atomist.rug.cli.utils.StringUtils.puralize("archive",
                                        operations.keySet())
                                + " found)" : ""));

        if (operations.isEmpty()) {
            log.info(Style.yellow("  No matching archives found"));
            log.newline();
        }
        else {
            operations.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
                    .forEach(a -> printArchive(a.getValue(), showOps));
            if (Constants.isShell()) {
                log.info("\nFor more information on specific archive version, run:\n"
                        + "  shell ARCHIVE -a VERSION\nfollowed by:\n  describe archive");
            }
            else {
                log.info("\nFor more information on specific archive version, run:\n"
                        + "  %sdescribe archive ARCHIVE -a VERSION", Constants.command());
            }
        }
    }

    private void printArchive(List<Operation> operations, boolean showOps) {
        Operation op = operations.get(0);
        Archive archive = op.archive();
        log.info("  %s %s(%s)", Style.yellow("%s:%s", archive.group(), archive.artifact()),
                (archive.scope() != null ? Style.gray("[" + archive.scope() + "]") + " " : ""),
                archive.version().value());
        if (showOps) {
            printOperations(operations);
        }
    }

    private void printOperations(List<Operation> operations) {
        Node node = new Node(null);
        addOperation(node, operations, "generator", "Generators");
        addOperation(node, operations, "editor", "Editors");
        addOperation(node, operations, "reviewer", "Reviewers");
        addOperation(node, operations, "command-handler", "Command Handlers");
        addOperation(node, operations, "event-handler", "Event Handlers");
        addOperation(node, operations, "respond-handler", "Respond Handlers");
        LogVisitor visitor = new LogVisitor(log);
        node.accept(visitor);
    }
    
    private void addOperation(Node node, List<Operation> operations, String kind, String label) {
        Collection<Operation> ops = operations.stream().filter(o -> o.type().equals(kind))
                .sorted(Comparator.comparing(Operation::name)).collect(Collectors.toList());
        if (!ops.isEmpty()) {
            Node parent = node.addChild(Style.bold(label), Type.UNKNOWN);
            ops.forEach(o -> parent.addChild(o.name(), Type.UNKNOWN));
        }
    }

    private List<String> getCatalogServiceUrls(Settings settings) {
        return settings.getConfigValue(CATALOG_URL_KEY, CATALOG_URL);
    }
}