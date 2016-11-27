package com.atomist.rug.cli.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.atomist.rug.cli.Log;

public class LogVisitor implements NodeVisitor {

    private List<ChildInfo> childInfos = new ArrayList<>();

    private Log out;

    private String indent = "  ";

    public LogVisitor(Log out) {
        this.out = out;
    }

    public LogVisitor(Log out, String indent) {
        this.out = out;
        this.indent = indent;
    }

    public boolean visitEnter(Node node) {
        if (node.id() != null) {
            out.info(indent + formatIndentation() + formatNode(node));
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

    private String formatIndentation() {
        StringBuilder buffer = new StringBuilder(128);
        for (Iterator<ChildInfo> it = childInfos.iterator(); it.hasNext();) {
            buffer.append(it.next().formatIndentation(!it.hasNext()));
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

        public String formatIndentation(boolean end) {
            boolean last = index + 1 >= count;
            if (end) {
                return last ? "└─ " : "├─ ";
            }
            return last ? "   " : "|  ";
        }
    }
}
