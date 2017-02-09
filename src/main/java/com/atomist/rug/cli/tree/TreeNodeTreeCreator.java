package com.atomist.rug.cli.tree;

import java.util.Collection;
import java.util.function.Function;

import com.atomist.rug.cli.tree.Node.Type;
import com.atomist.tree.TreeNode;

import scala.collection.JavaConverters;

public abstract class TreeNodeTreeCreator {

    public static void visitTree(Collection<TreeNode> sources, Function<TreeNode, String> nodeToString, NodeVisitor visitor) {
        if (sources == null) {
            return;
        }

        Node root = new Node(null);
        sources.forEach(s -> addNode(root, s, nodeToString));
        root.accept(visitor);
    }

    private static void addNode(Node parent, TreeNode node, Function<TreeNode, String> nodeToString) {
        Node newNode = parent.addChild(id(node, nodeToString), Type.UNKNOWN);
        JavaConverters.asJavaCollectionConverter(node.childNodes()).asJavaCollection()
                .forEach(c -> addNode(newNode, c, nodeToString));
    }

    private static String id(TreeNode node, Function<TreeNode, String> nodeToString) {
        return nodeToString.apply(node);
    }
}
