package com.atomist.rug.cli.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.atomist.rug.cli.Constants;

public class LogVisitor implements NodeVisitor {

    private List<ChildInfo> childInfos = new ArrayList<>();

    private StringBuilder out;

    private String indent = "  ";

    public LogVisitor(StringBuilder out) {
        this.out = out;
    }

    public LogVisitor(StringBuilder out, String indent) {
        this.out = out;
        this.indent = indent;
    }

    public boolean visitEnter(Node node) {
        if (node.id() != null) {
            out.append(indent + formatIndentation(node) + formatNode(node)).append(System.lineSeparator());
        }
        childInfos.add(new ChildInfo(node.children().size()));
        return true;
    }

    public void visitLeave(Node node) {
        if (!childInfos.isEmpty()) {
            childInfos.remove(childInfos.size() - 1);
        }
        if (!childInfos.isEmpty()) {
            childInfos.get(childInfos.size() - 1).index++;
        }
    }

    private String formatIndentation(Node node) {
        StringBuilder buffer = new StringBuilder(128);
        for (Iterator<ChildInfo> it = childInfos.iterator(); it.hasNext();) {
            buffer.append(it.next().formatIndentation(node, !it.hasNext()));
        }
        return buffer.toString();
    }

    private String formatNode(Node node) {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(node.id());
        return buffer.toString();
    }

    private static class ChildInfo {

        final int count;

        int index;

        public ChildInfo(int count) {
            this.count = count;
        }

        public String formatIndentation(Node node, boolean end) {
            boolean last = index + 1 >= count;
            if (end) {
                return last
                        ? (!node.children().isEmpty() ? Constants.LAST_TREE_NODE_WITH_CHILDREN
                                : Constants.LAST_TREE_NODE)
                        : (!node.children().isEmpty() ? Constants.TREE_NODE_WITH_CHILDREN
                                : Constants.TREE_NODE);
            }
            return last ? (Constants.TREE_CONNECTOR.length() == 2 ? "  " : "   ")
                    : Constants.TREE_CONNECTOR;
        }
    }
}
