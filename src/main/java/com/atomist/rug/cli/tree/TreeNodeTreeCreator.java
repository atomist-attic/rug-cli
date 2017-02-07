package com.atomist.rug.cli.tree;

import java.util.Collection;

import org.springframework.util.StringUtils;

import com.atomist.rug.cli.tree.Node.Type;
import com.atomist.tree.TreeNode;

import scala.collection.JavaConverters;

public abstract class TreeNodeTreeCreator {

    public static void visitTree(Collection<TreeNode> sources, NodeVisitor visitor) {
        if (sources == null) {
            return;
        }

        Node root = new Node(null);
        sources.forEach(s -> addNode(root, s));
        root.accept(visitor);
    }

    private static void addNode(Node parent, TreeNode node) {
        Node newNode = parent.addChild(id(node), Type.UNKNOWN);
        JavaConverters.asJavaCollectionConverter(node.childNodes()).asJavaCollection()
                .forEach(c -> addNode(newNode, c));
    }

    private static String id(TreeNode node) {
        return node.nodeName() + ": [" + StringUtils.collectionToDelimitedString(
                JavaConverters.asJavaCollectionConverter(node.nodeTags()).asJavaCollection(), ", ") + "]";
    }

}
