package com.atomist.rug.cli.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Node {

    private List<Node> children = new ArrayList<>();
    private String id;
    private Node parent;
    private Type type;

    public Node(Node parent) {
        this.parent = parent;
    }

    public void accept(NodeVisitor visitor) {
        visitor.visitEnter(this);
        children.stream().sorted(Comparator.comparing(Node::id))
                .forEach(c -> c.accept(visitor));
        visitor.visitLeave(this);
    }

    public Node addChild(Node node) {
        children.remove(node.parent);
        node.parent.children.remove(node);
        children.add(node);
        node.parent = this;
        return node;
    }

    public Node addChild(String id, Type type) {
        Node node = new Node(this);
        node.setId(id);
        node.setType(type);
        children.add(node);
        return node;
    }

    public List<Node> children() {
        return children;
    }

    public String id() {
        return id;
    }

    public Node parent() {
        return parent;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type type() {
        return type;
    }

    public enum Type {
        DIRECTORY, FILE
    }

}
