package com.atomist.rug.cli.tree;

import com.atomist.graph.GraphNode;
import com.atomist.rug.cli.tree.Node.Type;
import com.atomist.tree.TerminalTreeNode;
import scala.collection.JavaConverters;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public abstract class TreeNodeTreeCreator {

    public static void visitTree(Collection<GraphNode> sources,
            BiFunction<Integer, GraphNode, String> nodeToString, NodeVisitor visitor) {
        if (sources == null) {
            return;
        }

        Map<GraphNode, Integer> counts = count(sources);
        Node root = new Node(null);
        AtomicInteger counter = new AtomicInteger(0);
        Map<GraphNode, Integer> processed = new HashMap<>();
        sources.forEach(s -> addNode(root, s, nodeToString, processed, counter, counts));
        root.accept(visitor);
    }

    private static Map<GraphNode, Integer> count(Collection<GraphNode> sources) {
        AtomicInteger counter = new AtomicInteger(0);
        Map<GraphNode, Integer> processed = new HashMap<>();
        Map<GraphNode, Integer> counts = new HashMap<>();
        sources.forEach(s -> collectNodes(s, processed, counts, counter));
        return counts;
    }

    private static void collectNodes(GraphNode node, Map<GraphNode, Integer> processed,
            Map<GraphNode, Integer> counts, AtomicInteger counter) {
        if (!(node instanceof TerminalTreeNode)) {
            if (!processed.containsKey(node)) {
                int id = counter.incrementAndGet();
                processed.put(node, id);
                counts.put(node, 1);
                JavaConverters.asJavaCollectionConverter(node.relatedNodes()).asJavaCollection()
                        .forEach(c -> collectNodes(c, processed, counts, counter));
            }
            else {
                counts.put(node, counts.get(node) + 1);
            }
        }
        else {
            if (!processed.containsKey(node)) {
                processed.put(node, 0);
                counts.put(node, 1);
            }
            else {
                counts.put(node, counts.get(node) + 1);
            }
        }
    }

    private static void addNode(Node parent, GraphNode node,
            BiFunction<Integer, GraphNode, String> nodeToString,
            Map<GraphNode, Integer> processedNodes, AtomicInteger counter,
            Map<GraphNode, Integer> counts) {

        if (!(node instanceof TerminalTreeNode)) {
            if (!processedNodes.containsKey(node)) {
                int id = counter.incrementAndGet();
                processedNodes.put(node, id);
                Node newNode = parent.addChild(id(id(node, id, counts), node, nodeToString),
                        Type.UNKNOWN);
                JavaConverters.asJavaCollectionConverter(node.relatedNodes()).asJavaCollection()
                        .forEach(c -> addNode(newNode, c, nodeToString, processedNodes, counter,
                                counts));
            }
            else {
                int id = processedNodes.get(node);
                parent.addChild(id(id(node, id, counts), node, nodeToString), Type.UNKNOWN);
            }
        }
        else {
            parent.addChild(id(-1, node, nodeToString), Type.UNKNOWN);
        }
    }

    private static int id(GraphNode node, int id, Map<GraphNode, Integer> counts) {
        if (counts.containsKey(node) && counts.get(node) > 1) {
            return id;
        }
        else {
            return -1;
        }
    }

    private static String id(int id, GraphNode node,
            BiFunction<Integer, GraphNode, String> nodeToString) {
        return nodeToString.apply(id, node);
    }
}
