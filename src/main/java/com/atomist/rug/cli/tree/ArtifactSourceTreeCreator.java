package com.atomist.rug.cli.tree;

import static scala.collection.JavaConversions.asJavaCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.atomist.rug.cli.tree.Node.Type;
import com.atomist.rug.kind.core.DirectoryMutableView;
import com.atomist.rug.kind.core.FileMutableView;
import com.atomist.rug.kind.core.ProjectMutableView;
import com.atomist.source.Artifact;
import com.atomist.source.ArtifactSource;
import com.atomist.util.Visitable;
import com.atomist.util.Visitor;

public class ArtifactSourceTreeCreator {

    public static void visitTree(ArtifactSource source, NodeVisitor visitor) {
        if (source == null) {
            return;
        }

        ProjectMutableView view = new ProjectMutableView(source, source);
        Node root = new Node(null);
        view.accept(new Visitor() {

            @Override
            public boolean visit(Visitable visitable, int arg1) {
                if (visitable instanceof FileMutableView
                        || visitable instanceof DirectoryMutableView) {
                    getNodeForVisitable(visitable);
                }
                return true;
            }

            private Node getNodeForVisitable(Visitable visitable) {
                Artifact a;
                List<String> pathElements = null;
                Type type = null;
                if (visitable instanceof FileMutableView) {
                    a = ((FileMutableView) visitable).currentBackingObject();
                    pathElements = new ArrayList<>(asJavaCollection(a.pathElements()));
                    pathElements.add(a.name());
                    type = Type.FILE;
                }
                else if (visitable instanceof DirectoryMutableView) {
                    a = ((DirectoryMutableView) visitable).currentBackingObject();
                    pathElements = new ArrayList<>(asJavaCollection(a.pathElements()));
                    type = Type.DIRECTORY;
                }
                return getOrAddNode(root, pathElements, 0, type);
            }

            private Node getOrAddNode(Node node, List<String> pathElements, int depth, Type type) {
                if (depth >= pathElements.size()) {
                    return node;
                }
                else {
                    String id = pathElements.get(depth);
                    Optional<Node> childNode = node.children().stream()
                            .filter(c -> c.id().equals(id)).findFirst();
                    if (childNode.isPresent()) {
                        return getOrAddNode(childNode.get(), pathElements, depth + 1, type);
                    }
                    else {
                        return getOrAddNode(node.addChild(id, type), pathElements, depth + 1, type);
                    }
                }
            }

        }, 0);

        // Flatten empty DIRECTORY nodes
        flatten(root);
        root.accept(visitor);
    }

    private static void flatten(Node node) {
        Node parent = node.parent();
        String id = node.id();
        Optional<Node> files = node.children().stream().filter(n -> n.type().equals(Type.FILE))
                .findFirst();
        new ArrayList<>(node.children()).forEach(n -> {
            if (!files.isPresent() && Type.DIRECTORY.equals(node.type())) {
                parent.addChild(n);
                n.setId(id + "/" + n.id());
            }
            flatten(n);
        });

    }

}
