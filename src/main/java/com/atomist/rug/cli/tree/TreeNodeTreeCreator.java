package com.atomist.rug.cli.tree;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import com.atomist.rug.cli.tree.Node.Type;
import com.atomist.tree.TerminalTreeNode;
import com.atomist.tree.TreeNode;

import scala.collection.JavaConverters;

public abstract class TreeNodeTreeCreator {

    public static void visitTree(Collection<TreeNode> sources,
            BiFunction<Integer, TreeNode, String> nodeToString, NodeVisitor visitor) {
        if (sources == null) {
            return;
        }

        Map<TreeNode, Integer> counts = count(sources);
        Node root = new Node(null);
        AtomicInteger counter = new AtomicInteger(0);
        Map<TreeNode, Integer> processed = new HashMap<>();
        sources.forEach(s -> addNode(root, s, nodeToString, processed, counter, counts));
        root.accept(visitor);
    }

    private static Map<TreeNode, Integer> count(Collection<TreeNode> sources) {
        AtomicInteger counter = new AtomicInteger(0);
        Map<TreeNode, Integer> processed = new HashMap<>();
        Map<TreeNode, Integer> counts = new HashMap<>();
        sources.forEach(s -> collectNodes(s, processed, counts, counter));
        return counts;
    }

    private static void collectNodes(TreeNode node, Map<TreeNode, Integer> processed,
            Map<TreeNode, Integer> counts, AtomicInteger counter) {
        if (!(node instanceof TerminalTreeNode)) {
            if (!processed.containsKey(node)) {
                int id = counter.incrementAndGet();
                processed.put(node, id);
                counts.put(node, 1);
                JavaConverters.asJavaCollectionConverter(node.childNodes()).asJavaCollection()
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

    private static void addNode(Node parent, TreeNode node,
            BiFunction<Integer, TreeNode, String> nodeToString,
            Map<TreeNode, Integer> processedNodes, AtomicInteger counter,
            Map<TreeNode, Integer> counts) {

        if (!(node instanceof TerminalTreeNode)) {
            if (!processedNodes.containsKey(node)) {
                int id = counter.incrementAndGet();
                processedNodes.put(node, id);
                Node newNode = parent.addChild(id(id(node, id, counts), node, nodeToString),
                        Type.UNKNOWN);
                JavaConverters.asJavaCollectionConverter(node.childNodes()).asJavaCollection()
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

    private static int id(TreeNode node, int id, Map<TreeNode, Integer> counts) {
        if (counts.containsKey(node) && counts.get(node) > 1) {
            return id;
        }
        else {
            return -1;
        }
    }

    private static String id(int id, TreeNode node,
            BiFunction<Integer, TreeNode, String> nodeToString) {
        return nodeToString.apply(id, node);
    }
}
