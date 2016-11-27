package com.atomist.rug.cli.tree;

public interface NodeVisitor {

    boolean visitEnter(Node node);

    void visitLeave(Node node);

}